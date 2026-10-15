# Inicio: pulido visual del 14 de octubre de 2026

Se corrigieron la jerarquía tipográfica, el espaciado de letras, la alineación izquierda del encabezado y la distribución de los accesos. Ver actividad comparte la columna de la señal emocional; fecha y Ver avisos comparten la fila inferior. El estado de reproducción queda centrado y sus botones ocupan el ancho disponible. La barra usa reloj, contenido y calendario, como el boceto.

Se integraron el logo PNG transparente actualizado y `referencias/fondo-logros.jpg`, entregados por el usuario. El fondo se muestra sin deformación, dentro de una tarjeta de altura determinada por su texto, con borde dorado. Ambos originales se conservan.

## Evidencia

- [Captura superior](s25-inicio-pulido-arriba-2026-10-14.png).
- [Captura inferior](s25-inicio-pulido-2026-10-14.png).
- Samsung Galaxy S25, modelo SM-S931B, resolución 1080 × 2340, densidad física 480 dpi, densidad configurada 420 dpi, escala de fuente 1,0 y navegación por botones.
- Compilación `:app:assembleDebug`: correcta. Pruebas `:app:testDebugUnitTest`: 116 pruebas, 0 fallos y 0 errores.
- APK actualizado e instalado con conservación de datos.
- Los controles principales y los accesos mantienen una altura mínima de 48 dp; los botones de reproducción miden 126 px de alto con la densidad configurada de 420 dpi.

Las capturas corresponden a TEO Vista previa, con ejemplos y el mismo layout y renderer de Inicio. Se revisaron encabezado, logo, personaje, globo, tipografía, botones, fondo de celebración, aviso y navegación. El último aviso requiere desplazamiento; no se comprimió el contenido para imitar la altura del boceto.

Esta revisión no completa las pruebas con el peluche ni la validación de ventana de 360 dp y texto ampliado. T034 y T035 mantienen esas comprobaciones pendientes.

## Segunda corrección: anchos y fondo actualizado

Se incorporó la nueva versión de `referencias/fondo-logros.jpg`, con la zona izquierda despejada. Se quitó el límite de 200 dp del texto de la última señal: ahora ocupa su columna completa y usa el estilo de cuerpo de 15 sp. La cola del globo queda centrada.

Los estados se reparten el ancho disponible en partes iguales y comparten los bordes externos con Dormir TEO. Con fuente ampliada o ventana estrecha conservan el apilado vertical, con ancho completo y sin pesos horizontales.

La [captura de esta corrección](s25-inicio-detalles-2026-10-14.png) y la jerarquía de vistas del S25 confirman:

- Estados: desde x=406 hasta x=1006, igual que Dormir TEO; ancho total de 600 px, incluido el espacio entre ambos.
- Anchos individuales de 289 y 290 px: la diferencia de un píxel corresponde al reparto de píxeles enteros.
- Texto de la última señal: desde x=362 hasta x=1006, alcanzando el borde derecho de su columna.
- Compilación correcta y 116 pruebas con 0 fallos y 0 errores. Actualización instalada conservando los datos.
