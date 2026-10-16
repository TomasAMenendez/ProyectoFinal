# Inicio: selector de estados en depuración

La vista previa estaba fijada en Conectado y Despierto. Ahora su aviso dorado superior abre Estado de Inicio, con siete ejemplos: Despierto, Dormido, Preparándose, Desconectado, Música, Cuento y Sin datos. El selector y los ejemplos pertenecen a `src/debug`; no envían comandos, no consultan al peluche ni guardan datos de producción.

Cada opción usa `HomeStatus`, `NowPlaying`, `HomeScreenRenderer` y el mismo layout de Inicio. Las etiquetas largas se apilan cuando su ancho medido no permite conservarlas completas en la fila; la variante Despierto conserva dos estados con el ancho total del botón.

## Comprobación en Samsung Galaxy S25

Se recorrieron las siete opciones y se comprobaron sus etiquetas y controles. La evidencia automática de esa revisión de interfaz está en [s25-estados-preview-2026-10-14.json](s25-estados-preview-2026-10-14.json).

- [Despierto](s25-inicio-estado-despierto-2026-10-14.png): Dormir TEO disponible; Detener deshabilitado.
- [Dormido](s25-inicio-estado-dormido-2026-10-14.png): Despertar TEO disponible.
- [Preparándose](s25-inicio-estado-preparandose-2026-10-14.png): Despertar TEO deshabilitado.
- [Desconectado](s25-inicio-estado-desconectado-2026-10-14.png): controles de encendido y reproducción deshabilitados, sin datos actuales.
- [Música](s25-inicio-estado-musica-2026-10-14.png): archivo visible y Detener habilitado.
- [Cuento](s25-inicio-estado-cuento-2026-10-14.png): título visible, Volver a leer cuento y Detener habilitados.
- [Sin datos](s25-inicio-estado-sin-datos-2026-10-14.png): ausencia de interacciones y señal, sin inventar un cero.

Después de detectar un salto de letra en Desconectado, se corrigió la adaptación de las etiquetas y se volvió a comprobar Desconectado y Despierto. Compilación correcta y 121 pruebas con 0 fallos y 0 errores. Actualización instalada conservando datos.

La app principal conserva sus conexiones y el recorrido de primer uso. Su puerta de vinculación puede redirigir a la búsqueda de TEO si no hay conexión LAN; el selector de depuración permite inspeccionar Inicio desconectado sin cambiar ese comportamiento. La ejecución física con el peluche sigue pendiente.
