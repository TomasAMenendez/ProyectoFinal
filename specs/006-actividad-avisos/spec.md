# Actividad y Avisos

## Pantallas

Actividad reemplaza el destino de métricas. Es secundaria: regreso, título «Actividad» y descripción «Los momentos compartidos con TEO». No repite la barra inferior, la campana ni el engranaje.

Orden: selector Hoy / Semana / Mes; resumen del período; interacciones por día; áreas; vocabulario.

Avisos completa el destino existente: regreso, título «Avisos» y descripción «Novedades y mensajes de TEO». Cada tarjeta tiene etiqueta, icono, fecha u hora válida y el mensaje completo.

## Actividad

Hoy, Semana y Mes consultan los rangos ya existentes de 1, 7 y 30 días. La etiqueta describe ese rango, no un mes calendario.

Al cambiar de período se ocultan los números anteriores hasta que llega la respuesta de ese pedido. Una respuesta vieja no pisa a la más reciente. Un cero presente en la respuesta es un dato. Una clave ausente o una consulta fallida no se muestran como cero. La carga, el período vacío, el error con reintento y la desconexión son estados distintos.

Los registros de malestar usan solo `crisis_count`. Las áreas son conteos. Puede existir la categoría general, así que las cuatro áreas visibles no se fuerzan a sumar el total. El gráfico de treinta días se desplaza en horizontal y mantiene la letra.

El total acumulado de vocabulario se muestra solo si la respuesta trae una fuente confirmada. El rango que ya consulta la app manda `events` vacío y el resumen no incluye ese total: en ese caso dice «Sin datos». No se modifica el peluche ni el servidor.

## Avisos

Se reutilizan la colección en memoria y las reglas de ParentInbox: hasta veinte, orden por fecha del evento y la recepción solo como respaldo para ordenar. La recepción no se presenta como fecha. Si la fecha no es válida, se omite. No hay otra consulta que vacíe la cola, ni historial persistente, ni leído, ni eliminar, ni archivar.

Sin conexión se conservan los avisos ya recibidos y se indica que no llegan nuevos. La lista vacía no afirma que nunca hubo eventos. Los mensajes que llegan de TEO se muestran tal cual. Las etiquetas son Logro, Pedido, Atención y Aviso.

## Vista previa

Usa los mismos layouts. «Ver actividad» abre Actividad. La campana y «Ver avisos» abren Avisos. Los ejemplos cubren datos, vacío, carga, error y desconexión, sin consultar a TEO ni escribir datos reales.
