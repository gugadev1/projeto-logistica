const form = document.getElementById("simulacao-form");
const alertBox = document.getElementById("alert");
const bar = document.getElementById("ocupacao-bar");
const ocupacaoLabel = document.getElementById("ocupacao-label");
const kpiCarga = document.getElementById("kpi-carga");
const kpiVolume = document.getElementById("kpi-volume");
const kpiPlaca = document.getElementById("kpi-placa");
const romaneioBody = document.getElementById("romaneio-body");

// -----------------------------------------------------------------------
// Seletor de modo origem
// -----------------------------------------------------------------------
let origemModo = "cep";
let pacotesModo = "coord";

document.querySelectorAll("#origem-modo-cep, #origem-modo-coord").forEach((btn) => {
  btn.addEventListener("click", () => {
    origemModo = btn.dataset.modo;
    document.querySelectorAll("#origem-modo-cep, #origem-modo-coord").forEach((b) =>
      b.classList.toggle("ativo", b === btn)
    );
    document.getElementById("origem-cep-fields").classList.toggle("hidden", origemModo !== "cep");
    document.getElementById("origem-coord-fields").classList.toggle("hidden", origemModo !== "coord");
  });
});

document.querySelectorAll("#pacotes-modo-cep, #pacotes-modo-coord").forEach((btn) => {
  btn.addEventListener("click", () => {
    pacotesModo = btn.dataset.modo;
    document.querySelectorAll("#pacotes-modo-cep, #pacotes-modo-coord").forEach((b) =>
      b.classList.toggle("ativo", b === btn)
    );
    const hint = document.getElementById("pacotes-hint");
    hint.textContent = pacotesModo === "cep"
      ? "id,peso,volume,CEP,prioridade  |  ou  id,peso,volume,CEP,fallbackX,fallbackY,prioridade"
      : "id,peso,volume,x,y,prioridade";
    const area = document.getElementById("pacotes");
    area.value = pacotesModo === "cep"
      ? "PX-001,15,1.8,01310100,EXPRESSO\nPX-002,20,2.1,04538133,3"
      : "PX-001,15,1.8,2,3,EXPRESSO\nPX-002,20,2.1,5,1,3\nPX-003,8,0.9,1,1,2";
  });
});

// -----------------------------------------------------------------------
// Submit
// -----------------------------------------------------------------------
form.addEventListener("submit", async (event) => {
  event.preventDefault();
  ocultarAlerta();

  try {
    const payload = construirPayload();
    const response = await fetch("/api/simular", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    const data = await response.json();

    if (!response.ok) {
      exibirAlerta(mensagemAmigavel(data), "error");
      return;
    }

    atualizarOcupacao(data.ocupacaoPercentual);
    kpiCarga.textContent = `${data.cargaAtual.toFixed(2)} / ${data.cargaMax.toFixed(2)} kg`;
    kpiVolume.textContent = `${data.volumeAtual.toFixed(2)} / ${data.volumeMax.toFixed(2)} m3`;
    kpiPlaca.textContent = data.placa;
    renderizarRomaneio(data.romaneio);
    exibirAlerta("Simulacao executada com sucesso.", "success");
  } catch (error) {
    exibirAlerta("Erro ao processar dados. Verifique o formato dos campos.", "error");
  }
});

// -----------------------------------------------------------------------
// Construir payload
// -----------------------------------------------------------------------
function construirPayload() {
  const base = {
    placa: document.getElementById("placa").value,
    cargaMax: Number(document.getElementById("cargaMax").value),
    volumeMax: Number(document.getElementById("volumeMax").value),
    cargaInicial: Number(document.getElementById("cargaInicial").value),
  };

  if (origemModo === "cep") {
    const cep = normalizarCep(document.getElementById("origemCep").value);
    validarFormatoCep(cep, "CEP de origem");
    base.origemCep = cep;
    const fx = document.getElementById("origemFallbackX").value;
    const fy = document.getElementById("origemFallbackY").value;
    if (fx !== "" && fy !== "") {
      base.origemX = Number(fx);
      base.origemY = Number(fy);
    }
  } else {
    base.origemX = Number(document.getElementById("origemX").value);
    base.origemY = Number(document.getElementById("origemY").value);
  }

  base.pacotes = construirPacotes();
  return base;
}

function construirPacotes() {
  const linhas = document
    .getElementById("pacotes")
    .value
    .split("\n")
    .map((l) => l.trim())
    .filter((l) => l.length > 0);

  if (linhas.length === 0) throw new Error("Nenhum pacote informado.");

  return linhas.map((linha, idx) => {
    const campos = linha.split(",").map((c) => c.trim());
    const pos = idx + 1;

    if (pacotesModo === "cep") {
      // formatos aceitos: id,peso,volume,CEP,prioridade  ou  id,peso,volume,CEP,fbX,fbY,prioridade
      if (campos.length !== 5 && campos.length !== 7) {
        throw new Error(`Pacote ${pos}: formato invalido. Use id,peso,volume,CEP,prioridade ou id,peso,volume,CEP,fbX,fbY,prioridade.`);
      }
      const cep = normalizarCep(campos[3]);
      validarFormatoCep(cep, `Pacote ${pos}`);
      const pacote = { id: campos[0], peso: Number(campos[1]), volume: Number(campos[2]), cep, prioridade: campos[campos.length - 1] };
      if (campos.length === 7) {
        pacote.coordenadaX = Number(campos[4]);
        pacote.coordenadaY = Number(campos[5]);
      }
      return pacote;
    } else {
      if (campos.length !== 6) {
        throw new Error(`Pacote ${pos}: formato invalido. Use id,peso,volume,x,y,prioridade.`);
      }
      return {
        id: campos[0],
        peso: Number(campos[1]),
        volume: Number(campos[2]),
        coordenadaX: Number(campos[3]),
        coordenadaY: Number(campos[4]),
        prioridade: campos[5],
      };
    }
  });
}

// -----------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------
function normalizarCep(valor) {
  return (valor || "").replace(/\D/g, "");
}

function validarFormatoCep(cep, contexto) {
  if (cep.length !== 8) {
    throw new Error(`${contexto}: CEP deve ter 8 digitos numericos. Recebido: "${cep}".`);
  }
}

function mensagemAmigavel(data) {
  if (!data || !data.error) return "Falha na simulacao.";
  switch (data.error) {
    case "ENDERECO_INVALIDO":
      return data.message || "CEP invalido ou nao encontrado. Confira e tente novamente.";
    case "SERVICO_GEOCODIFICACAO_INDISPONIVEL":
      return (data.message || "Servico de geolocalização indisponivel.") + " Informe coordenadas manuais como fallback.";
    case "CAPACIDADE_EXCEDIDA":
      return data.message || "Capacidade do veiculo excedida.";
    case "REQUISICAO_INVALIDA":
      return data.message || "Dados invalidos. Verifique os campos.";
    default:
      return data.message || "Erro inesperado. Tente novamente.";
  }
}

function atualizarOcupacao(ocupacaoPercentual) {
  const percentual = Math.max(0, Math.min(ocupacaoPercentual, 100));
  bar.style.width = `${percentual}%`;
  ocupacaoLabel.textContent = `${percentual.toFixed(1)}%`;

  bar.classList.remove("yellow", "red");
  if (percentual > 90) {
    bar.classList.add("red");
  } else if (percentual > 70) {
    bar.classList.add("yellow");
  }
}

function renderizarRomaneio(itens) {
  if (!itens || itens.length === 0) {
    romaneioBody.innerHTML = "<tr><td colspan=\"6\" class=\"empty\">Sem itens no romaneio.</td></tr>";
    return;
  }

  romaneioBody.innerHTML = itens
    .map(
      (item, index) => `
      <tr>
        <td>${index + 1}</td>
        <td>${item.id}</td>
        <td>${item.prioridade}</td>
        <td>${item.distancia.toFixed(2)}</td>
        <td>${item.peso.toFixed(2)}</td>
        <td>${item.volume.toFixed(2)}</td>
      </tr>
    `,
    )
    .join("");
}

function exibirAlerta(mensagem, tipo) {
  alertBox.textContent = mensagem;
  alertBox.classList.remove("hidden", "error", "success");
  alertBox.classList.add(tipo);
}

function ocultarAlerta() {
  alertBox.classList.add("hidden");
  alertBox.classList.remove("error", "success");
}
