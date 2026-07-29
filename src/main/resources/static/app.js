let currentLang = 'es';
let processedExpenses = [];
let manualIncome = [];
let nextRowId = 0;

let iaeDataCache = [];

const translations = {
    es: {
        title: "Calculadora Fiscal",
        subtitle: "Análisis automatizado de facturas. Privado y sin estado.",
        sectorLabel: "Actividad Económica (Código CNAE)",
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
        statusRejected: "[RECHAZADO]",
        statusReview: "[REVISIÓN]",
        statusOk: "[OK]",
        aiReasoning: "IA Auditor:",
        pending: "Pendiente",
        alertPdf: "Formato no válido. Solo se admiten archivos PDF."
    },
    en: {
        title: "Tax Calculator",
        subtitle: "Automated invoice analysis. Private and stateless.",
        sectorLabel: "Economic Activity (CNAE Code)",
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
        statusRejected: "[REJECTED]",
        statusReview: "[REVIEW]",
        statusOk: "[OK]",
        aiReasoning: "AI Auditor:",
        pending: "Pending",
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
    loadCnaeData();
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
    }
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

async function loadCnaeData() {
    const cacheKey = 'iae_codes_istac_v1';
    const datosGuardados = localStorage.getItem(cacheKey);

    if (datosGuardados) {
        iaeDataCache = JSON.parse(datosGuardados);
        buildBrowser();
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
        buildBrowser();
    } catch (error) {
        console.error("Error cargando el IAE:", error);
    }
}

function buildBrowser() {
    iaeList.innerHTML = '';
    iaeDataCache.forEach(actividad => {
        const option = document.createElement('option');
        option.value = `[${actividad.id}] ${actividad.nombre}`;
        iaeList.appendChild(option);
    });
}

function getTaxProfile(inputText) {
    const match = inputText.match(/\[(.*?)\]/);
    if (!match) return "GENERIC";

    const code = match[1];

        if (code.startsWith("4932")) return "TAXI_VTC";
        if (code.startsWith("8553")) return "DRIVING_SCHOOL";
        if (code.startsWith("49") || code.startsWith("53")) return "TRANSPORT";
        if (code.startsWith("461")) return "COMMERCIAL_AGENT";
        if (code.startsWith("96")) return "STUDIO_BASED";
        if (code.startsWith("62") || code.startsWith("63")
            || code.startsWith("70") || code.startsWith("74")) return "DESK_BASED";

    return "GENERIC";
}

let dragDepth = 0;

dropzone.addEventListener('dragenter', (e) => {
    e.preventDefault();
    dragDepth++;
    dropzone.classList.add('dragover');
});

dropzone.addEventListener('dragover', (e) => e.preventDefault());

dropzone.addEventListener('dragleave', () => {
    dragDepth--;
    if (dragDepth <= 0) { dragDepth = 0; dropzone.classList.remove('dragover'); }
});

dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');


const fileInput = document.getElementById('fileInput');

dropzone.addEventListener('click', () => fileInput.click());
dropzone.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInput.click(); }
});

    fileInput.addEventListener('change', () => {
    for (let file of fileInput.files) {
        if (file.type === "application/pdf") processInvoice(file);
        else alert(translations[currentLang].alertPdf);
    }
    fileInput.value = '';
});


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
    const perfilJava = getTaxProfile(sectorInput.value);

const rowId = `row-${nextRowId++}`;
const li = document.createElement('li');
li.id = rowId;
li.innerHTML = `
    <span><strong>${translations[currentLang].statusProcessing}</strong> ${file.name}</span>
    <span class="row-actions">
        <span class="row-amount">...</span>
        <button type="button" class="row-remove" aria-label="Eliminar">&times;</button>
    </span>`;
fileList.prepend(li);

li.querySelector('.row-remove').addEventListener('click', (e) => {
    e.stopPropagation();
    processedExpenses = processedExpenses.filter(item => item.__rowId !== rowId);
    li.remove();
    calculateQuarter();
});

data.__rowId = rowId;
processedExpenses.push(data);

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

        if (!data.isValid) {
            li.innerHTML = `<span><strong>${translations[currentLang].statusRejected}</strong> ${file.name}</span>
                            <span style="color: var(--text-secondary); font-size: 0.85em;">${data.manualReviewReason || 'N/A'}</span>`;
            li.style.borderColor = "var(--border-color)";

            processedExpenses.push(data);

            return;
        }

        if (data.requiresManualReview) {
            li.innerHTML = `
                <div style="width: 100%;">
                    <div style="display: flex; justify-content: space-between;">
                        <span><strong>${translations[currentLang].statusReview}</strong> ${file.name} (${data.category})</span>
                        <span>+0.00€ (${translations[currentLang].pending})</span>
                    </div>
                    <div class="warning-box" style="margin-top: 0.5rem; width: 100%; border-left: 3px solid var(--text-primary);">
                        <strong>${translations[currentLang].aiReasoning}</strong> ${data.aiReasoning}<br>
                        <em>Aviso: ${data.manualReviewReason}</em>
                    </div>
                </div>`;

                processedExpenses.push(data);

            return;
        }

        processedExpenses.push(data);

        li.innerHTML = `<span><strong>${translations[currentLang].statusOk}</strong> ${file.name} | ${data.category}</span>
                        <span>+${data.deductibleVat.toFixed(2)}€</span>`;

    } catch (error) {
        console.error("Fallo de red o servidor:", error);
        li.innerHTML = `<span><strong>${translations[currentLang].statusError}</strong> ${file.name}</span> <span>0.00€</span>`;
        li.style.color = "var(--text-secondary)";
    }
}

function addManualIncome() {
    const date = document.getElementById('incomeDate').value;
    const amount = document.getElementById('incomeAmount').value;

    if (!date || !amount || Number(amount) <= 0) {
        alert("Introduce fecha e importe válidos.");
        return;
    }

    const entry = {
        date: date,
        amount: Number(amount),
        vatRate: Number(document.getElementById('incomeVatRate').value),
        clientType: document.getElementById('incomeClientType').value,
        paymentMethod: document.getElementById('incomePaymentMethod').value
    };

    manualIncome.push(entry);
    renderIngresos();
    calcularTrimestre();

    document.getElementById('incomeAmount').value = '';
}

function renderIncome() {
    const list = document.getElementById('incomeList');
    list.innerHTML = '';

    manualIncome.forEach((entry, index) => {
        const li = document.createElement('li');
        li.innerHTML = `
            <span>${entry.date} · ${entry.paymentMethod}</span>
            <span class="row-actions">
                <span class="row-amount">${entry.amount.toFixed(2)} €</span>
                <button type="button" class="row-remove" aria-label="Eliminar">&times;</button>
            </span>`;
        li.querySelector('.row-remove').addEventListener('click', () => {
            manualIncome.splice(index, 1);
            renderIncome();
            calculateQuarter();
        });
        list.appendChild(li);
    });
}

async function calculateQuarter() {
    if (manualIncome.length === 0 && processedExpenses.length === 0) {
        resetDashboard();
        return;
    }

    const payload = {
        profile: {
            iaeCode: (sectorInput.value.match(/\[(.*?)\]/) || [null, ""])[1],
            iaeSection: document.getElementById('iaeSection').value,
            deductionProfile: getTaxProfile(sectorInput.value),
            monthlyRetaFee: Number(document.getElementById('retaFee').value) || 0,
            activityStartYear: Number(document.getElementById('startYear').value) || new Date().getFullYear()
        },
        quarter: document.getElementById('quarterSelect').value,
        year: Number(document.getElementById('yearSelect').value),
        income: manualIncome,
        expenses: processedExpenses
    };

    try {
        const response = await fetch('/api/quarter/summary', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error(`El servidor respondió ${response.status}`);

        drawDashboard(await response.json());
    } catch (error) {
        console.error("Fallo al calcular el trimestre:", error);
    }
}

function drawDashboard(summary) {
    const eur = (n) => Number(n).toFixed(2) + " €";

    document.getElementById('totalGross').innerText = eur(summary.totalBilled);
    document.getElementById('totalVat').innerText = eur(summary.vatToPay);
    document.getElementById('totalIrpf').innerText = eur(summary.irpfPrePayment);
    document.getElementById('totalNet').innerText = eur(summary.avalibleCash);

    const note = document.getElementById('excludedNote');
    if (summary.excludedDocuments > 0) {
        note.innerText = `${summary.excludedDocuments} factura(s) fuera del cálculo por revisión pendiente.`;
        note.hidden = false;
    } else {
        note.hidden = true;
    }
}

function resetDashboard() {
    ['totalGross', 'totalVat', 'totalIrpf', 'totalNet']
        .forEach(id => document.getElementById(id).innerText = "0.00 €");
    document.getElementById('excludedNote').hidden = true;
}