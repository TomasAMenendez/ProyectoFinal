# TEO · Entrega para compilar y probar

Preparada el 15 de octubre de 2026.

La variante **debug es la app real**: descubre a TEO y envía y recibe datos por su API. La variante **preview** tiene otro identificador y utiliza datos simulados. Para usar el peluche, elegir **debug** y abrir **TEO**.

## Obtener esta versión desde GitHub

La entrega está en la rama **001-inicio-base-visual** de [ProyectoFinal](https://github.com/pololo500/ProyectoFinal/tree/001-inicio-base-visual).

Para obtener una copia independiente:

```sh
git clone --branch 001-inicio-base-visual --single-branch https://github.com/pololo500/ProyectoFinal.git
```

Abrir `ProyectoFinal/ProyectoAndroid` en Android Studio y seguir los pasos de compilación de esta guía, con la variante `debug`. Los APK y ZIP generados se mantienen como archivos locales; para probar desde GitHub se compila el código de esta rama.

Para actualizar una Raspberry existente desde esa copia, detener TEO, hacer una copia de respaldo y copiar únicamente los archivos `.py`, `requirements.txt`, `intent_rules.json` y las carpetas `assets`, `eyes` y `juegos` de `ProyectoParaRasperrypiV5`. Conservar los datos y configuración enumerados en «Preparar el peluche». No reemplazar toda la carpeta existente con los ejemplos del repositorio.

## Archivos para compartir

- `TEO-Android-2026-10-15.zip`: proyecto Android completo, con recursos y Gradle Wrapper.
- `TEO-real-debug.apk`: app real ya compilada para instalar y probar.
- `TEO-peluche-actualizacion-2026-10-15.zip`: código Python y recursos para actualizar una instalación existente de TEO.
- `VALIDACION.md`: comprobaciones realizadas y prueba física pendiente.
- `manifest.json`: inventario y huellas SHA-256 de los paquetes.

El proyecto Android no contiene rutas locales del SDK/JDK, cachés, claves de firma privadas ni credenciales del peluche. La actualización Python conserva fuera del paquete los datos y la configuración particular del aparato.

## Abrir y ejecutar en Android Studio

1. Extraer el ZIP Android en una carpeta propia. Abrir **ProyectoAndroid**, donde están `settings.gradle.kts` y `gradlew.bat`.
2. Permitir la sincronización y descarga de dependencias. El proyecto utiliza AGP 9.2.1 y Gradle Wrapper 9.4.1. Si Android Studio avisa que no admite ese plugin, usar una versión compatible; no aceptar una migración que cambie las versiones sin necesidad.
3. En SDK Manager, instalar la plataforma **Android SDK 36.1**, Build Tools 36.0.0 y Platform Tools. Aceptar sus licencias.
4. El proyecto solicita **JetBrains JDK 21** mediante `gradle-daemon-jvm.properties`. Usar el JDK integrado compatible o permitir la descarga del toolchain. No copiar `local.properties` de otra PC: Android Studio crea el suyo.
5. En **Build Variants**, seleccionar `debug` para `app`. Elegir la configuración de ejecución `app` y el teléfono conectado.
6. Activar depuración USB, autorizar esa PC desde el teléfono y pulsar **Run**. Abrir **TEO**. Esta compilación no agrega el lanzador de vista previa.

Para compilar desde la terminal de Android Studio:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. En macOS/Linux se usa `./gradlew`; si perdió el permiso al extraerlo, ejecutar `chmod +x gradlew`.

La combinación de herramientas está documentada en [compatibilidad oficial de AGP 9.2](https://developer.android.com/build/releases/agp-9-2-0-release-notes). El JDK 21 es la configuración elegida por este proyecto.

## Instalar el APK entregado

Se puede abrir `TEO-real-debug.apk` en el teléfono y permitir su instalación desde el gestor de archivos, o usar:

```powershell
adb install -r TEO-real-debug.apk
```

`-r` conserva los datos si la firma coincide. Una compilación hecha en otra PC puede utilizar otra clave debug. Si aparece `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, la app instalada tiene una firma distinta: conservar sus datos y usar el APK entregado o la misma clave autorizada. No desinstalar para resolverlo si se necesita conservar la Agenda local.

El APK debug permite probar el producto real; para una distribución pública se necesita una firma release propia. `assembleRelease` se comprobó, pero no se creó una clave de publicación.

## Preparar el peluche

Instalar un APK no actualiza la Raspberry. Esta entrega incluye correcciones en la API, el scheduler de rutinas y la sesión de cuentos, además de los pictogramas. Para usarlas hay que actualizar también el software de TEO.

El ZIP Python es una **actualización de una instalación que ya funciona**. No incluye modelos, entorno virtual, credenciales, configuración de pines ni datos del niño. Tampoco actualiza ServidorDePC.

1. Detener TEO con su mecanismo habitual y guardar una copia de su carpeta antes de actualizar.
2. Extraer el ZIP en una carpeta aparte. Copiar el contenido de `ProyectoParaRasperrypiV5` sobre la carpeta de código existente, conservando las subcarpetas.
3. Mantener los archivos locales `.env`, `hardware_pins.json`, `pairing_token.txt`, `child_profile.json`, `parental_prefs.json`, `routines_config.json`, `vocabulary_data.json`, `pc_server_url.txt`, `pc_server_token.txt`, música, cuentos, telemetría, modelos y `venv`. Esos archivos no se entregan ni se reemplazan.
4. En el entorno virtual existente, comprobar la dependencia de PDF:

```sh
./venv/bin/python -c "import pypdf; print(pypdf.__version__)"
```

Si falta, instalarla en ese mismo entorno:

```sh
./venv/bin/python -m pip install pypdf
```

5. Reiniciar TEO con el mismo lanzador y entorno que ya usaba. Mantener disponible ServidorDePC si la instalación lo usa para voz y conversación. No volver a ejecutar la preparación completa de la Raspberry para aplicar esta actualización.
6. Comprobar que la API escucha en el puerto habitual 8080. Desde otra máquina de la misma red se puede abrir `http://IP-DE-TEO:8080/api/status`. Una respuesta válida acredita la API; todavía no prueba audio ni pantalla.

Los PNG de rutina deben quedar en `assets/pictograms/`: `banio.png`, `cepillar_dientes.png`, `comer.png`, `jardin.png` y `medico.png`.

## Primera conexión real

1. Encender TEO y conectar el teléfono a la misma red Wi-Fi del peluche. Una red de invitados con aislamiento entre dispositivos puede impedir la comunicación aunque comparta nombre o router.
2. Abrir **TEO** y conceder los permisos de dispositivos cercanos. En Android antiguo también puede pedir ubicación para buscar por Bluetooth. Las notificaciones permiten recibir avisos y recordatorios.
3. Si TEO ya tiene Wi-Fi, el descubrimiento LAN debe encontrarlo. Si todavía no tiene red, la app ofrece configurarlo por Bluetooth.
4. En ese flujo, ingresar el PIN de seis dígitos mostrado en la pantalla del peluche, elegir una de las redes que detecta **TEO** y escribir su contraseña.
5. Esperar la confirmación LAN. Si el teléfono está en otra red, conectarlo a la red elegida y usar el reintento. La conexión Bluetooth por sí sola no significa que la API esté accesible.
6. Completar el perfil cuando corresponda. Elegir el disfraz con nombre e imagen y guardar; la app actualiza sus imágenes cuando el perfil queda confirmado.

Si no encuentra a TEO, revisar encendido, permisos, red y logs del peluche. Cuando se pueda entrar a Mi TEO, Conexión → Diagnóstico avanzado permite comprobar la dirección e intentar una conexión manual. No se muestran datos de ejemplo en la app real para ocultar una desconexión.

## Recorrido para validar con el aparato

| Paso | Resultado a comprobar |
| --- | --- |
| Inicio | Conectado/desconectado, despierto/dormido y preparándose siguen el estado real. |
| Dormir y despertar | El peluche cambia de estado; un despertar pendiente no aparece como listo. |
| Celebrar | La app confirma el envío aceptado y el peluche ejecuta el festejo. |
| Rutinas | Crear, editar, activar/desactivar y eliminar; reiniciar y comprobar que siguen guardadas. |
| Pictograma | Programar una rutina unos minutos adelante, con un PNG. Al empezar el audio del recordatorio principal aparece centrado ocho segundos y luego vuelven los ojos. Repetir sin pictograma. |
| Día siguiente | Una rutina completada vuelve a estar disponible al cambiar de día. |
| Música | Elegir un audio del teléfono, subirlo, reproducirlo, detenerlo y eliminarlo. Repetir con un nombre con espacios o acentos. |
| Cuentos | Elegir un PDF con texto seleccionable de hasta 8 MB, subir, editar título/texto, leer y detener incluso al empezar la narración. |
| Ajustes | Aplicar volumen/brillo, comprobar su efecto y reiniciar TEO para verificar que se conservan. Probar modo noche. |
| Perfil | Guardar nombre, preferencias y disfraz; reabrir la app y comprobar la selección e imágenes. |
| Actividad y Avisos | Mostrar datos reales; un aviso nuevo llega una vez y conserva su tipo y fecha cuando es válida. |
| Agenda | Crear un turno o fecha de vacuna, cerrar y reabrir; verificar conservación y recordatorio del teléfono. |
| Segundo plano | Con la app en segundo plano, generar un aviso desde TEO y comprobarlo con permisos concedidos. Revisar las restricciones de batería del teléfono si Android suspende la entrega. |
| Recuperación | Interrumpir y restablecer la red: debe informar la pérdida y recuperarse sin perder un formulario abierto. |

La Agenda se guarda en ese teléfono. Los avisos recientes tienen un máximo de veinte durante la sesión. No hay promesa de ejecución física basada solamente en un HTTP exitoso: comparar cada acción con lo que hace TEO.

## Si algo falla

Anotar el paso exacto, la hora, la versión Android y lo que mostró la app. Guardar una captura y el fragmento de log del peluche correspondiente, sin incluir contraseñas, PIN ni tokens. Para un cierre de la app, adjuntar la excepción `AndroidRuntime` de Logcat. El informe `VALIDACION.md` separa las comprobaciones locales de las físicas pendientes.
