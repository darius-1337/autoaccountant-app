# AutoAccountant

*[English version](README.en.md)*

> **Proyecto en desarrollo.** Algunas funciones descritas aquí todavía no están completas. Consulta el apartado *Estado actual* para saber qué funciona hoy. Esta nota es temporal.

Calculadora fiscal trimestral para autónomos en España. Lee facturas de gasto con un modelo de inteligencia artificial, las clasifica según la actividad económica declarada y desglosa lo que hay que reservar para Hacienda.

La idea de partida es sencilla: quien factura 30.000 euros al año no se lleva 30.000 euros. Entre el IVA repercutido, el pago fraccionado del IRPF y la cuota de autónomos, la diferencia entre lo que se ingresa y lo que queda disponible es grande, y no siempre está clara hasta que llega el trimestre. Esta aplicación intenta enseñar esa diferencia.

![Panel de resultados de un trimestre](docs/img/panel-resultados.png)

## Antes de usarla

**Esto no es asesoramiento fiscal.** Los cálculos son orientativos y no sustituyen a un gestor o asesor profesional. No uses estos números para presentar un modelo 303 o 130 sin que los revise un profesional.

Riesgos concretos que conviene tener presentes:

- **La IA se equivoca leyendo.** El modelo puede confundir un importe, asignar mal un tipo de IVA o clasificar un gasto en la categoría equivocada. Cada factura procesada debe revisarse. La aplicación marca las dudosas, pero puede cometer errores.
- **En modo nube, los datos pasan por Google.** La versión desplegada usa la API gratuita de Gemini, y en ese nivel gratuito Google puede utilizar el contenido enviado para mejorar sus modelos. Si vas a procesar facturas reales con datos sensibles, ejecuta el proyecto en local con un modelo propio.
- **Solo contempla estimación directa.** Si tributas por módulos (estimación objetiva), el sistema de cálculo es completamente distinto y esta aplicación no sirve.
- **Las operaciones extranjeras no están resueltas.** Las facturas de proveedores fuera de España con inversión del sujeto pasivo (suscripciones de software, servicios intracomunitarios) se procesan, pero el tratamiento fiscal que aplica todavía no es correcto.
- **No guarda nada.** Al cerrar la pestaña se pierde todo. Es intencionado por privacidad, pero significa que no hay historial ni forma de recuperar una sesión.

## Cómo se usa

**1. Elige tu actividad económica.** El buscador carga la lista de epígrafes del IAE publicada por el ISTAC. Selecciona el que tengas declarado en el modelo 036 o 037, no el que mejor describa lo que haces: son cosas distintas y a veces el modelo entiende otra cosa.

**2. Completa tu perfil.** Necesita la cuota mensual que pagas al RETA (aparece en el recibo de la Seguridad Social) y el año en que te diste de alta. Con eso puede calcular la provisión de IRPF y saber si te corresponde la retención reducida.

**3. Sube las facturas.** Arrastra los PDF a la zona de subida. Se procesan una a una y el estado de cada una aparece debajo.

![Facturas procesadas con distintos estados](docs/img/facturas-procesadas.png)

**4. Revisa lo marcado.** Algunos gastos no se pueden resolver automáticamente: los relacionados con vehículos, los suministros de la vivienda, las dietas y las atenciones a clientes dependen de datos que una factura por sí sola no contiene. Esas facturas quedan fuera del cálculo hasta que las confirmes. Si te equivocaste al subir algo, elimínalo y los totales se recalculan solos.

**5. Lee el resultado.** El panel muestra el total facturado, el IVA que hay que reservar y el dinero que queda realmente disponible.

## Cosas que conviene saber

**La IA extrae, pero no decide.** El modelo se encarga de leer la factura y clasificar el gasto. Los porcentajes de deducción los aplica el backend con reglas fijas escritas en Java. Esto es deliberado: un modelo de lenguaje puede alucinar un porcentaje, y aquí hay dinero de por medio. Cuando la IA sugiere un porcentaje, la factura queda marcada para revisión manual.

**IVA e IRPF se calculan por separado.** No siempre coinciden. El caso más claro es el de un vehículo de uso mixto: se deduce la mitad del IVA pero nada en IRPF, porque la ley no admite la afectación parcial de un vehículo en el impuesto sobre la renta.

**El epígrafe del IAE y las reglas de gasto son cosas distintas.** El epígrafe sirve para declarar y para saber si tus facturas llevan retención. Las reglas de deducción se aplican según un perfil de actividad separado. Un tatuador, por ejemplo, tiene que darse de alta en epígrafes pensados para estética o servicios personales, y aplicarle las reglas de gasto de un esteticista sería incorrecto.

**No hay base de datos.** Los archivos entran en memoria, se procesan y se descartan. No se almacena ninguna factura ni ningún dato personal en el servidor.

**El despliegue gratuito se duerme.** En Render, el servicio se suspende tras unos minutos sin uso. La primera petición después de ese periodo puede tardar cerca de un minuto en responder. No está roto, está arrancando.

**Hay un límite de peticiones por IP** para no agotar la cuota gratuita de la API, Gemini tiene su propio limite configurable, pero he decidido usar tambien la libreria Bucket4j. Si subes muchas facturas seguidas, algunas se rechazarán temporalmente.

## Estado actual

El procesamiento de gastos funciona. La entrada de ingresos todavía no: hasta que esté, la aplicación clasifica gastos correctamente pero no puede cerrar un trimestre completo, porque el IVA repercutido y el rendimiento neto dependen de lo facturado.

El modo local con un modelo propio está previsto en la arquitectura pero no implementado. La interfaz del servicio de IA está desacoplada para que añadir otro proveedor no obligue a tocar el resto del código.

## Configuración

Variables de entorno necesarias:

| Variable | Descripción |
|---|---|
| `GEMINI_API_KEY` | Clave de la API en Google AI Studio |
| `GEMINI_API_MODEL` | Modelo a usar. Por defecto, el Flash más reciente |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos. En local no hace falta tocarlo |

## Stack

Java con Spring Boot en el backend, HTML, CSS y JavaScript sin frameworks en el frontend, servido como una sola aplicación. Sin base de datos.
