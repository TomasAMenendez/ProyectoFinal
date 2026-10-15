# Propuestas de Mi TEO

Fecha: 13 de octubre de 2026. Etapa: preparación de bocetos, sin implementación.

## Mi TEO P17

Imagen recibida: [Mi TEO propuesto](mi-teo-propuesta-2026-10-13.png). Se conserva como propuesta visual; todavía no hay aprobación explícita ni implementación.

Se propone un menú secundario al que se accede desde el engranaje de las pantallas principales. Usa el estilo de [Rutinas elegido](rutinas-elegida-2026-10-13.png), con flecha para volver y título Mi TEO. No tiene barra inferior ni un engranaje que abra el propio menú.

Incluye tres tarjetas de navegación: Ajustes de TEO → P18 (volumen, brillo y modo noche); Conexión → P19 (estado de conexión y Wi-Fi); Perfil del niño → P03 (nombre, intereses y personalización). El diagnóstico P20 se abre desde Conexión como opción avanzada. No se muestran los formularios dentro del menú ni se agregan cuentas, pagos, múltiples perfiles o restablecimiento de fábrica. Corresponde al alcance P17 del [inventario](../01-inventario-y-pantallas.md).

El escenario del boceto muestra TEO conectado, mediante un indicador discreto en la tarjeta Conexión. El pie muestra Versión de la app: 1.0, que coincide con el [versionName actual](../../../ProyectoAndroid/app/build.gradle.kts). Al implementar, la versión debe obtenerse de la compilación, no fijarse a partir de la imagen. El límite de uso y el pulsador mantienen las decisiones pendientes registradas en el inventario.

## Ajustes de TEO P18

Imagen recibida: [Ajustes de TEO propuesto](ajustes-teo-propuesta-2026-10-13.png). Se conserva como propuesta visual, sin aprobación explícita ni implementación. Contiene volumen máximo, brillo de la pantalla de TEO y modo noche. Se reutilizan el fondo marfil, los colores y las tarjetas de Mi TEO, con flecha de regreso y sin barra inferior. La imagen agrega la versión de la app al pie; si se conserva, se obtiene de la compilación como en P17.

Según [ConfigViewModel](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/config/ConfigViewModel.kt), volumen y brillo se envían al pulsar Aplicar cambios. El modo noche se envía por separado al accionar el interruptor; no depende de ese botón. Los dos deslizadores actuales usan 0–100 % y pasos de 5 en el [layout de configuración](../../../ProyectoAndroid/app/src/main/res/layout/fragment_config.xml). Los valores 60 % y 70 % del prompt son ejemplos visuales, no datos leídos del peluche.

El botón Aplicar cambios queda dentro de la tarjeta de volumen y brillo. El modo noche se muestra en otra tarjeta, con interruptor desactivado y una explicación breve: «Limita el volumen y prioriza el descanso». La [política de sesión](../../../ProyectoParaRasperrypiV5/session_policy.py) limita el volumen a un máximo de 40 % durante modo noche y restringe las intenciones atendidas. Las [rutinas de aplicación](../../../ProyectoParaRasperrypiV5/app.py) detienen la música al activarlo. No se presenta como el tema oscuro de la app ni se promete un horario automático.

El límite de uso y el despertar mediante pulsador conservan las decisiones pendientes del inventario; no se incluyen en este boceto. Tampoco se agregan controles de voz, cámara, servos o reinicio de fábrica. Al implementar, la pantalla deberá cargar el estado real, confirmar los envíos y mostrar los errores sin anunciar cambios exitosos antes de la respuesta.

## Conexión P19

Imagen recibida: [Conexión propuesta](conexion-propuesta-2026-10-13.png). Se conserva como propuesta visual, sin aprobación explícita ni implementación. Respeta los controles previstos. Al implementar, reducir un poco la altura y el protagonismo del acceso a Diagnóstico avanzado, que en la imagen ocupa casi una tarjeta principal. La versión del pie se obtiene de la compilación si se conserva.

Se propone una pantalla secundaria con flecha para volver a Mi TEO, título Conexión y el mismo estilo de Ajustes. El primer escenario muestra una conexión vigente con TEO. Una tarjeta contiene el indicador Conectado, el texto «Tu teléfono puede comunicarse con TEO» y la acción secundaria Volver a buscar TEO. Otra tarjeta muestra Red Wi-Fi de TEO, Red actual: Casa (dato ficticio) y Cambiar red Wi-Fi. Debajo queda un acceso discreto a Diagnóstico avanzado P20. No se exponen IP, puertos o claves en esta pantalla principal de conexión.

La búsqueda, el estado real y el envío de un cambio de red ya tienen soporte en [RobotConnectionManager](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotConnectionManager.kt) y [ConfigFragment](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/config/ConfigFragment.kt). Su distribución en una vista separada es la propuesta de rediseño. El cambio abre la selección de red y contraseña, reutilizando el recorrido P02; la búsqueda no equivale a reiniciar ni a desconectar físicamente el peluche.

La [API Wi-Fi](../../../ProyectoParaRasperrypiV5/api_server.py) devuelve las redes disponibles y un campo current generado en [app.py](../../../ProyectoParaRasperrypiV5/app.py). Según [wifi_provision.py](../../../ProyectoParaRasperrypiV5/wifi_provision.py), current es el nombre del perfil de conexión de NetworkManager, que puede diferir del SSID. Al implementar, verificar el nombre mostrado como red actual; no inventarlo ni conservarlo como vigente si no se pudo consultar.

En la variante desconectada, el estado será Desconectado y la acción principal Buscar TEO. No se afirmará que la red actual sigue disponible ni se habilitará un cambio por HTTP sin comunicación; la recuperación por BLE se orientará a P01/P02. La búsqueda debe mostrar progreso indeterminado y permitir reintentar si falla. Estas variantes se conservan dentro de P19, sin sumar vistas al inventario. No se agregan botones de reiniciar, desvincular ni garantías de cifrado o seguridad sin respaldo.

## Diagnóstico avanzado P20

Imagen recibida: [Diagnóstico avanzado propuesto](diagnostico-avanzado-propuesta-2026-10-13.png). Se conserva como propuesta visual, sin aprobación explícita ni implementación. Distingue la información de solo lectura del campo editable de conexión manual y respeta el alcance previsto. Las direcciones de la imagen son ficticias, no valores para fijar en la app.

Se propone una pantalla secundaria desde Conexión, con flecha de regreso, el mismo estilo visual y la descripción «Información técnica y conexión manual». Una tarjeta de Información de conexión contiene estado Conectado, IP del teléfono 192.168.1.20, IP de TEO 192.168.1.50 y puerto 8080. Las IP son ejemplos ficticios para el boceto. Otra tarjeta de Conexión manual contiene un campo Dirección IP de TEO con 192.168.1.50 y el botón Conectar manualmente. La ayuda será «Usá esta opción si la búsqueda automática no encuentra a TEO». El pie muestra la versión real de la app. No se agregan otras funciones técnicas.

Las fuentes son ConfigFragment y RobotConnectionManager, enlazados en P19, junto con [RobotApiClient](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotApiClient.kt). El código obtiene la IP del teléfono, conserva la dirección configurada, consulta información del servicio y admite conexión por una IP introducida manualmente. El puerto predeterminado es 8080; se muestra como información, sin inventar un selector de puerto. Al implementar, mostrar el puerto efectivo si el descubrimiento configuró otro y distinguir la dirección guardada de una nueva dirección escrita todavía sin verificar.

Actualmente parte de la información técnica se muestra solo en el banner DEBUG. Distribuirla en P20 es una propuesta de interfaz; no implica que esa pantalla exista en la app actual. Se omiten instrucciones de consola, referencias al emulador, tokens de vinculación, contraseñas, registros y garantías de seguridad. No se agregan exportación de registros, pruebas de hardware, actualizaciones ni reinicios. La conexión manual debe mostrar el intento y su resultado confirmado; guardar una dirección no equivale a comprobar que TEO respondió.
