const form = document.getElementById("simulacao-form");
const alertBox = document.getElementById("alert");
const bar = document.getElementById("ocupacao-bar");
const ocupacaoLabel = document.getElementById("ocupacao-label");
const kpiCarga = document.getElementById("kpi-carga");
const kpiVolume = document.getElementById("kpi-volume");
const kpiPlaca = document.getElementById("kpi-placa");
const romaneioBody = document.getElementById("romaneio-body");

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  ocultarAlerta();

  try {
    const payload = construirPayload();
    const response = await fetch("/api/simular", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const data = await response.json();

    if (!response.ok) {
      exibirAlerta(data.message || "Falha na simulacao.", "error");
      return;
    }

    atualizarOcupacao(data.ocupacaoPercentual);
    kpiCarga.textContent = `${data.cargaAtual.toFixed(2)} / ${data.cargaMax.toFixed(2)} kg`;
    kpiVolume.textContent = `${data.volumeAtual.toFixed(2)} / ${data.volumeMax.toFixed(2)} m3`;
    kpiPlaca.textContent = data.placa;
    renderizarRomaneio(data.romaneio);
    exibirAlerta("Simulacao executada com sucesso.", "success");
  } catch (error) {
    exibirAlerta("Erro ao processar dados. Verifique o formato dos pacotes.", "error");
  }
});

function construirPayload() {
  const linhasPacote = document
    .getElementById("pacotes")
    .value
    .split("\n")
    .map((linha) => linha.trim())
    .filter((linha) => linha.length > 0);

  if (linhasPacote.length === 0) {
    throw new Error("Nenhum pacote informado.");
  }

  const pacotes = linhasPacote.map((linha) => {
    const campos = linha.split(",").map((campo) => campo.trim());
    if (campos.length !== 6) {
      throw new Error("Formato de pacote invalido.");
    }

    return {
      id: campos[0],
      peso: Number(campos[1]),
      volume: Number(campos[2]),
      coordenadaX: Number(campos[3]),
      coordenadaY: Number(campos[4]),
      prioridade: campos[5],
    };
  });

  return {
    placa: document.getElementById("placa").value,
    cargaMax: Number(document.getElementById("cargaMax").value),
    volumeMax: Number(document.getElementById("volumeMax").value),
    cargaInicial: Number(document.getElementById("cargaInicial").value),
    origemX: Number(document.getElementById("origemX").value),
    origemY: Number(document.getElementById("origemY").value),
    pacotes,
  };
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
