let totals = { bruto: 0, iva: 0, neto: 0 };

const dropzone = document.getElementById('dropzone');
const sectorSelect = document.getElementById('sector');
const fileList = document.getElementById('file-list');

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
            procesarFactura(file);
        } else {
            alert(`El archivo ${file.name} no es un PDF válido.`);
        }
    }
});

async function procesarFactura(file) {
    const sector = sectorSelect.value;

    const li = document.createElement('li');
    li.innerHTML = `<span class="status-icon">⚙️</span> Analizando <strong>${file.name}</strong>...`;
    fileList.prepend(li);

    const formData = new FormData();
    formData.append("archivo", file);
    formData.append("sector", sector);

    try {
        const response = await fetch('/api/extraer-datos', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error('Error procesando la factura en el servidor');
        }

        const data = await response.json();

        /*
           Estructura esperada del JSON de Java:
           {
               "base_imponible": 50.00,
               "iva_deducible": 10.50,
               "semaforo": "VERDE", // VERDE, AMARILLO, ROJO
               "mensaje": "100% deducible (Transporte)"
           }
        */

        totals.bruto += data.base_imponible;
        totals.iva += data.iva_deducible;

        totals.neto = totals.bruto - totals.iva - (totals.bruto * 0.15);

        document.getElementById('total-bruto').innerText = totals.bruto.toFixed(2) + " €";
        document.getElementById('total-iva').innerText = totals.iva.toFixed(2) + " €";
        document.getElementById('total-neto').innerText = totals.neto.toFixed(2) + " €";

        const iconos = {
            'VERDE': '🟢',
            'AMARILLO': '🟡',
            'ROJO': '🔴'
        };
        const icono = iconos[data.semaforo] || '⚪';

        li.innerHTML = `<span class="status-icon">${icono}</span>
                        <strong>${file.name}</strong>: ${data.mensaje}
                        <em>(+${data.iva_deducible.toFixed(2)}€ IVA)</em>`;

    } catch (error) {
        console.error(error);
        li.innerHTML = `<span class="status-icon">❌</span> Error al procesar <strong>${file.name}</strong>. Revisa la consola.`;
        li.classList.add('error');
    }
}