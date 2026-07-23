let currentLang = 'es';
let totals = { gross: 0, vat: 0, irpf: 0, net: 0 };
let iaeDataCache = [];

const translations = {
    es: {
        title: "Calculadora Fiscal",
        subtitle: "Análisis automatizado de facturas. Privado y sin estado.",
        sectorLabel: "Actividad Económica (Epígrafe IAE)",
        dropText: "Arrastrar facturas (PDF)",
        dropSubText: "Procesamiento local mediante IA",
        coldStart: "Aviso: El servidor gratuito puede tardar 50s en responder al primer archivo.",
        cardGross: "Ingreso Bruto",
        cardVat: "IVA (M303)",
        cardIrpf: "IRPF (M130)",
        cardNet: "Neto Disponible",
        historyTitle: "Registro de operaciones",
        footerText: "Aviso: Herramienta de estimación. No sustituye la asesoría fiscal profesional.",
        statusProcessing: "[PROCESANDO]",
        statusError: "[ERROR]",
        alertPdf: "Formato no válido. Solo se admiten archivos PDF."
    },
    en: {
        title: "Tax Calculator",
        subtitle: "Automated invoice analysis. Private and stateless.",
        sectorLabel: "Economic Activity (IAE Code)",
        dropText: "Drag & Drop invoices (PDF)",
        dropSubText: "Local AI processing",
        coldStart: "Notice: Free tier server may take up to 50s to wake up on the first file.",
        cardGross: "Gross Income",
        cardVat: "VAT (Q303)",
        cardIrpf: "IRPF (Q130)",
        cardNet: "Net Available",
        historyTitle: "Operations Log",
        footerText: "Disclaimer: Estimation tool. Does not replace professional tax advice.",
        statusProcessing: "[PROCESSING]",
        statusError: "[ERROR]",
        alertPdf: "Invalid format. Only PDF files are allowed."
    }
};

const themeToggle = document.getElementById('themeToggle');
const langSwitch = document.getElementById('langSwitch');
const sectorInput = document.getElementById('sectorInput');
const iaeList = document.getElementById('iaeList');
const dropzone = document.getElementById('dropzone');
const fileList = document.getElementById('fileList');

document.addEventListener("DOMContentLoaded", () => {
    cargarEpigrafesIAE();
    updateUILanguage();
});

themeToggle.addEventListener('click', () => {
    const html = document.documentElement;
    if (html.getAttribute('data-theme') === 'dark') {
        html.setAttribute('data-theme', 'light');
        themeToggle.innerText = 'dark_mode';
    } else {
        html.setAttribute('data-theme', 'dark');
        themeToggle.innerText = 'light_mode';
    }c
});

langSwitch.addEventListener('change', (e) => {
    currentLang = e.target.value;
    updateUILanguage();
});

function updateUILanguage() {
    const elements = document.querySelectorAll('[data-i18n]');
    elements.forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (translations[currentLang][key]) {
            el.innerHTML = translations[currentLang][key];
        }
    });
}

async function cargarEpigrafesIAE() {
    const cacheKey = 'iae_codes_istac_v1';
    const datosGuardados = localStorage.getItem(cacheKey);

    if (datosGuardados) {
        iaeDataCache = JSON.parse(datosGuardados);
        construirBuscador();
        return;
    }

    try {
        const response = await fetch('/codes.json');
        if (!response.ok) throw new Error("Error HTTP al cargar codes.json");

        const data = await response.json();

        iaeDataCache = data.code.map(item => {
            const nombreEs = item.name.text.find(t => t.lang === 'es')?.value || "Desconocido";
            return { id: item.id, nombre: nombreEs };
        });

        localStorage.setItem(cacheKey, JSON.stringify(iaeDataCache));
        construirBuscador();
    } catch (error) {
        console.error("Error cargando el IAE:", error);
    }
}

function construirBuscador() {
    iaeList.innerHTML = '';
    iaeDataCache.forEach(actividad => {
        const option = document.createElement('option');
        option.value = `[${actividad.id}] ${actividad.nombre}`;
        iaeList.appendChild(option);
    });
}

function obtenerPerfilFiscal(inputText) {
    const match = inputText.match(/\[(.*?)\]/);
    if (!match) return "GENERAL";

    const iaeCode = match[1];

    if (iaeCode.startsWith("1_72") || iaeCode.startsWith("1_38")) return "TRANSPORT";
    if (iaeCode.startsWith("1_76") || iaeCode.startsWith("1_39") || iaeCode.startsWith("1_84")) return "OFFICE_AND_TECH";

    return "GENERAL";
}

dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
});

dropzone.addEventListener('dragleave', () => {
    dropzone.classList.remove('dragover');
});

dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');

    const files = e.dataTransfer.files;
    for (let file of files) {
        if (file.type === "application/pdf") {
            processInvoice(file);
        } else {
            alert(translations[currentLang].alertPdf);
        }
    }
});

async function processInvoice(file) {
    const perfilJava = obtenerPerfilFiscal(sectorInput.value);

    const li = document.createElement('li');
    li.innerHTML = `<span><strong>${translations[currentLang].statusProcessing}</strong> ${file.name}</span> <span>...</span>`;
    fileList.prepend(li);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("sector", perfilJava);

    try {
        const response = await fetch('/api/invoices/process', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) throw new Error('Error en el servidor Java');

        const data = await response.json();

        totals.gross += data.originalTaxBase;
        totals.vat += data.deductibleVat;

        const netYield = data.originalTaxBase - data.deductibleTaxBase;
        totals.irpf += netYield > 0 ? (netYield * 0.20) : 0;

        totals.net = totals.gross - totals.vat - totals.irpf;

        actualizarDashboard();

        const textStatus = { 'GREEN': '[OK]', 'YELLOW': '[INFO]', 'RED': '[NO-DED]' };
        const statusMark = textStatus[data.statusColor] || '[...]';

        li.innerHTML = `<span><strong>${statusMark}</strong> ${file.name} | ${data.message}</span>
                        <span>+${data.deductibleVat.toFixed(2)}€</span>`;

    } catch (error) {
        console.error("Fallo de red o servidor:", error);
        li.innerHTML = `<span><strong>${translations[currentLang].statusError}</strong> ${file.name}</span> <span>0.00€</span>`;
        li.style.color = "var(--text-secondary)";
    }
}

function actualizarDashboard() {
    document.getElementById('totalGross').innerText = totals.gross.toFixed(2) + " €";
    document.getElementById('totalVat').innerText = totals.vat.toFixed(2) + " €";
    document.getElementById('totalIrpf').innerText = totals.irpf.toFixed(2) + " €";
    document.getElementById('totalNet').innerText = totals.net.toFixed(2) + " €";
}