# Plan: Actividad y Avisos

Se trabaja en la rama actual, sin commit y sin tocar el peluche, el servidor ni la composición de Inicio, Rutinas, Contenido, Agenda y Mi TEO.

Actividad sigue en `nav_metrics` y Avisos en `nav_alerts`. Ambos quedan en el tema claro y sin barra inferior. La presentación se comparte entre el fragmento y TEO Vista previa.

`MetricsPeriodParser` distingue claves presentes de claves ausentes. El total de vocabulario sale únicamente de un evento `vocabulary` que traiga `total_known_words`; si no está, queda vacío. `MetricsViewModel` limpia el período al pedir otro rango, descarta respuestas fuera de orden y separa carga, listo, error y desconexión.

El gráfico diario y las barras de área se arman en la vista, con desplazamiento horizontal cuando hay más de ocho días. Avisos redibujan la tarjeta existente y observan `parentAlerts` e `isConnected`. No agregan un poll nuevo.

La vista previa infla esos layouts con ejemplos en memoria. Reintentar un error de ejemplo no llama a la red.
