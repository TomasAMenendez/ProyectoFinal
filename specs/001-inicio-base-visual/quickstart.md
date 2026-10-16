# Quickstart: comprobar Inicio

Guía de revisión. No reemplaza las tareas de implementación. Los comandos se ejecutan cuando esa implementación exista.

## Antes de la captura

Anotar del Samsung Galaxy S25: versión de Android, tamaño lógico, escala de fuente, escala de pantalla y si la navegación es por gestos o botones. El modelo no alcanza para conocer el espacio útil.

## Comprobaciones automáticas

Desde `ProyectoAndroid`, cuando el código del bloque esté implementado:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Tienen que seguir pasando las pruebas existentes de primer uso, perfil y bandeja. Las nuevas tienen que cubrir, sin teléfono ni peluche:

- Despierto, dormido, preparándose y desconectado, con las acciones habilitadas que corresponden.
- `pending_wake` no se muestra como Despierto aunque `power_on` venga verdadero.
- Cero informado por la API distinto de consulta fallida o campo inválido, sin prometer distinguir un cero de respaldo.
- Señal ausente distinta de una señal registrada.
- Detener deshabilitado sin reproducción y habilitado con canción o cuento.
- Reproducir y detener leen el cuerpo y el estado posterior. Una solicitud rechazada, un error de consulta o un estado que no confirma no anuncian éxito ni borran lo conocido. La desconexión deja de presentar esa reproducción como vigente.
- Etiqueta «Volver a leer cuento» solo con un cuento activo.
- Catálogo vacío sin anunciar una reproducción.
- «Celebración enviada a TEO» solo cuando el cuerpo confirma el envío. Dormido o no disponible no se muestran como envío ni como festejo terminado.
- Mientras una acción sigue, no sale otra solicitud igual y la espera no muestra un porcentaje. Después de éxito o error, los controles coinciden con la disponibilidad real.
- Orden de avisos por fecha del evento o, si no es válida, por una recepción que no cambia al reabrir.
- Tope de veinte y fecha omitida cuando no es válida.
- Un encendido con despertar pendiente no se presenta como Despierto.

La compilación y estas pruebas no demuestran parecido visual ni que el peluche haya ejecutado la acción.

## Comprobación en el teléfono

Instalar la compilación de depuración en el S25. Revisar Inicio con el teléfono en modo claro y en modo oscuro.

Estados a observar, con peluche cuando hagan falta datos reales:

1. Conectado y despierto, sin reproducción: los siete bloques, en orden, con Detener visible y deshabilitado.
2. Dormir y ver Dormido solo después de la confirmación. Despertar y ver Preparándose si el estado queda pendiente.
3. Desconectado: sin afirmar despierto ni dormido.
4. Conteo numérico informado por la API, incluido cero, y ausencia cuando la consulta falla o el campo no es válido.
5. Música activa, cuento activo y catálogo vacío.
6. Celebración enviada, TEO dormido y función no disponible. El primer caso no dice que el festejo terminó.
7. Aviso con fecha, aviso sin fecha, lista clara con título y regreso a Inicio, sin la barra de actividad.
8. Ver actividad, el toque de Ver contenido, Mi TEO, Ver conexión, Rutinas y Agenda abren los destinos de este piloto.
9. Texto ampliado y una ventana de 360 dp: los controles se leen, se desplazan y no quedan bajo las barras del sistema. El mismo margen no aparece dos veces.

Comparar la captura con `docs/ui-ux/bocetos/inicio-principal-elegido-2026-10-13.png` y con `docs/ui-ux/04-guia-visual-comun.md`. Mirar logo, tamaño visible del personaje según su contenido opaco, proporciones, bordes sobre la tarjeta blanca, confeti separado del texto, globo, colores y orden. Anotar diferencias concretas. Un recurso sustituto, o un encuadre que solo use el lienzo completo, no cierra esta revisión.

Al entrar en Actividad, Contenido, Rutinas, Agenda o Configuración, comprobar que recuperan su apariencia beta. Inicio y la lista de avisos siguen claros en modo oscuro. El recorrido de conexión y perfil del primer uso sigue igual.
