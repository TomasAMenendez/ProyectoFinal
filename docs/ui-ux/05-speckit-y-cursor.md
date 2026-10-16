# TEO: desarrollar con Spec Kit y Cursor

Fecha: 13 de octubre de 2026. Guía adaptada al repositorio existente y al Samsung Galaxy S25 del usuario.

## Punto de partida comprobado

La raíz Git es C:/Users/Santi/Desktop/ProyectoFinal. ProyectoAndroid es un módulo de esa raíz, no otro repositorio. Cursor debe abrir ProyectoFinal para tener disponibles la documentación, los bocetos y el código. Android Studio puede abrir ProyectoAndroid para compilar y ejecutar.

Se comprobó que Specify CLI 1.0.4 funciona, con Python 3.11.16. Cursor, uv, Git y adb están disponibles en PATH. Todavía no existe .specify en este proyecto. No hace falta reinstalar Spec Kit para seguir esta guía.

La integración actual de Cursor usa .cursor/skills/speckit-<nombre>/SKILL.md. Los pasos del proceso se invocan en el chat de Cursor como /speckit-specify, con guion. No se escriben esos pasos en PowerShell. Los ejemplos antiguos con /speckit.specify corresponden a otro modo o versión; manda la integración que se haya instalado. Referencias: [integraciones oficiales](https://github.com/github/spec-kit/blob/main/docs/reference/integrations.md), [skills de Cursor](https://cursor.com/docs/skills).

Spec Kit organiza requisitos, decisiones, plan y tareas. Cursor produce y modifica el código. La fidelidad visual se comprueba con capturas reales y correcciones; Spec Kit no convierte una imagen automáticamente en componentes Android.

## 1. Abrir la carpeta e inicializar

Abrí C:/Users/Santi/Desktop/ProyectoFinal en Cursor. En su terminal PowerShell:

```powershell
Set-Location -LiteralPath 'C:\Users\Santi\Desktop\ProyectoFinal'
git status --short
specify version
specify init --here --integration cursor-agent --script ps
```

Es un proyecto con archivos existentes: init puede pedir confirmar que integre sus archivos en la carpeta. Se espera infraestructura .specify y nuevas skills speckit-* dentro de .cursor/skills. Ya hay otras skills en .cursor; se conservan. No se usa --force como primer intento ni se crea una app Android nueva. Revisá el diff de lo generado antes de continuar.

Esta inicialización la realizará el usuario desde Cursor; esta preparación solo agregó documentación y referencias visuales. El uso de --here, --integration y --script se verificó contra la ayuda local 1.0.4 y la [instalación oficial](https://github.com/github/spec-kit/blob/main/docs/installation.md). La [guía para proyectos existentes](https://github.com/github/spec-kit/blob/main/docs/guides/existing-projects.md) explica la adopción sobre código ya creado.

Para comprobar la integración después de init:

```powershell
specify integration status --json
Get-ChildItem -LiteralPath '.cursor/skills' -Directory -Filter 'speckit-*'
```

En el chat de Agent escribí /speckit- y elegí la skill del autocompletado. Si no aparece, recargá la ventana de Cursor y verificá que abrió la raíz correcta. No inventes comandos ni agregues más instalaciones hasta revisar qué archivos generó init.

Usaremos la integración del IDE. El ejecutable separado cursor-agent y el despliegue remoto no son necesarios para este recorrido.

## 2. Darle contexto a Cursor

Primero revisá la [guía visual](04-guia-visual-comun.md) y su [muestra](guia-visual.html). Sus tamaños y HEX son una propuesta normalizada para aprobar, no medidas mágicamente extraídas del PNG.

Adjuntá al chat de Cursor el archivo bocetos/inicio-principal-elegido-2026-10-13.png, arrastrándolo desde el explorador. Usá @ para seleccionar los documentos que corresponden al bloque. Para Inicio alcanza la guía visual, el inventario, la ficha de Inicio y el índice; el agente puede consultar después las notas específicas.

Pegá este contexto inicial:

```text
Trabajamos sobre el repositorio existente ProyectoFinal. El código móvil está en ProyectoAndroid y las referencias en docs/ui-ux.

Quiero implementar el rediseño de TEO con fidelidad a los bocetos y conservar las funciones actuales. La referencia principal de Inicio es docs/ui-ux/bocetos/inicio-principal-elegido-2026-10-13.png, que adjunto.

Leé docs/ui-ux/04-guia-visual-comun.md, docs/ui-ux/01-inventario-y-pantallas.md, docs/ui-ux/02-inicio-ficha-y-prompt.md y docs/ui-ux/03-indice-bocetos.md. La ficha inicial de Inicio conserva antecedentes; la imagen elegida más reciente y la guía común tienen prioridad visual.

El dispositivo principal de prueba es mi Samsung Galaxy S25. La app usa Kotlin, XML, ViewBinding y Material Components.

Primero resumí el alcance que entendiste y las dudas reales. Identificá los componentes compartidos, los recursos gráficos pendientes y los destinos de navegación actuales. No generes código de la app en este primer mensaje. Preguntame las decisiones de producto que falten; no vuelvas a preguntar lo que ya está confirmado en los documentos.
```

No hace falta adjuntar las veinte imágenes en cada conversación ni volver a analizar el DOCX, audios o material de otros proyectos. Cada bloque lleva sus imágenes y notas relevantes; esto reduce contexto repetido y evita mezclar decisiones.

## 3. Crear los principios una vez

En el chat de Cursor, invocá esta skill y enviá el texto junto con ella:

```text
/speckit-constitution

Establecé principios para el rediseño de la app móvil TEO dentro de este repositorio existente.

1. Mantener las funciones y contratos actuales; el alcance inicial es la interfaz Android. Los proyectos del peluche y del servidor son fuentes de consulta, no destinos de cambios automáticos.
2. Mantener Kotlin, XML, ViewBinding y Material Components. No migrar framework ni actualizar dependencias sin una necesidad concreta y revisada.
3. Usar la guía visual común de docs/ui-ux/04-guia-visual-comun.md, sus tokens y el boceto elegido de cada vista. La revisión de la guía precede a la implementación.
4. Construir componentes nativos reutilizables. No convertir una captura completa en la interfaz.
5. Mostrar estados y datos confirmados. Las respuestas HTTP no prueban por sí solas que el peluche ejecutó una acción. Los datos ficticios quedan limitados a previews o pruebas.
6. Respetar accesibilidad, áreas táctiles, texto ampliado, teclado e insets; validar en Samsung Galaxy S25.
7. Mantener las nueve skins y las cuatro secciones principales confirmadas. Tiempo de uso, pulsador y acceso sin TEO conservan sus decisiones pendientes.
8. Preguntar las decisiones de producto abiertas. Conservar las ya registradas y no inventar funciones, garantías de seguridad o datos que el código no proporciona.
9. Compilar, ejecutar comprobaciones pertinentes y comparar capturas reales antes de declarar una pantalla terminada.

Creá o actualizá solo la constitución y mostrámela para revisión.
```

Revisá .specify/memory/constitution.md. No debe quedar con nombres o principios genéricos de una plantilla ajena.

## 4. Primera especificación: base visual e Inicio

Conviene trabajar por bloques. El primer objetivo es comprobar en el S25 que los componentes pueden reproducir Inicio. El resto de las veinte vistas no se solicita como una única tarea gigante.

```text
/speckit-specify

Rediseñar la pantalla Inicio de la app móvil existente de TEO y establecer sus componentes visuales compartidos, usando docs/ui-ux/04-guia-visual-comun.md y docs/ui-ux/bocetos/inicio-principal-elegido-2026-10-13.png.

El adulto debe poder reconocer la conexión y actividad de TEO, dormirlo o despertarlo, consultar las interacciones y la última señal emocional registrada, iniciar música y detener la reproducción disponible, celebrar un logro y acceder a actividad, avisos, contenido, conexión y ajustes.

Conservar el orden y la jerarquía del boceto: encabezado, tarjeta de estado, Hoy con TEO con globo cian, Ahora con TEO, celebración dorada con confeti, avisos recientes y navegación Inicio/Rutinas/Contenido/Agenda.

Cubrir conectado/despierto, conectado/dormido, preparándose, desconectado, sin datos, reproducción activa, carga y error. Conservar el funcionamiento del primer uso y del perfil existente.

La primera revisión visual se hará en un Samsung Galaxy S25. El contenido debe adaptarse a la ventana, texto ampliado e insets, conservando funciones.

Excluir el rediseño completo de las otras vistas, cambios de backend, el criterio del tiempo de uso y el pulsador pendiente. No inventar rutas ni dejar botones decorativos: identificar los destinos existentes y las dependencias mínimas de navegación que falten, para resolverlas en la aclaración y el plan.

Usar datos ficticios únicamente en previews o pruebas. La app real debe mostrar sus datos y estados reales. Preparar la especificación y detenerse para que la revise.
```

Revisá las historias, el alcance y los criterios de aceptación del spec.md generado. Deben describir lo que hará el usuario, no una reescritura de toda la aplicación.

## 5. Aclarar y planificar antes de programar

```text
/speckit-clarify

Revisá la especificación activa con las referencias de docs/ui-ux. Preguntame solo ambigüedades que bloqueen este piloto. No reabras las decisiones aplazadas del tiempo ni del pulsador.

Prestá atención a los accesos de Inicio: algunas vistas actuales son beta y Mi TEO/Avisos son propuestas de organización nuevas. Indicá qué destinos pueden reutilizarse y qué contenedores mínimos habría que incorporar; acordemos ese alcance antes de implementarlos.
```

La aplicación tiene destinos beta que siguen funcionando. El piloto no debe crear pantallas vacías solo para que un botón navegue. La integración mínima de Mi TEO/Avisos se decide y registra en esta aclaración si resulta necesaria.

Después:

```text
/speckit-plan

Planificá la especificación activa sobre ProyectoAndroid. Conservá Kotlin, XML, ViewBinding, Fragments/ViewModels y Material Components actuales.

Mapeá la guía visual y teo-ui-tokens.json a recursos y estilos compartidos, inicialmente con nombres teo_. Revisá el alcance de la asignación global del tema para no recolorear sin revisión otras pantallas beta. No implementes el HTML de la guía dentro de un WebView.

Listá archivos actuales que se tocarían y cómo se conservarán los enlaces de Inicio, su primer uso y las operaciones existentes. Definí los estados a partir de RobotConnectionManager y la API real; verificá despertar pendiente, sueño, reproducción y errores.

Los recursos individuales de TEO ya están disponibles en docs/ui-ux/referencias/teo-saludando.png y docs/ui-ux/referencias/teo.png, agregados por el usuario el 14 de octubre de 2026; ambos tienen transparencia. La pose saludando coincide con el Inicio elegido. No planifiques recortar el oso de teo-y-skins.png: esa foto queda como antecedente. Incluí integración y revisión de los PNG individuales, el logo y el confeti antes de aceptar la fidelidad final. Señalá si hace falta algún recurso adicional que todavía no existe.

Incluí insets para targetSdk 36, desplazamiento, teclado y validación en Samsung Galaxy S25. Definí pruebas pertinentes de comportamiento y comparación visual; no agregues tests que solo repitan colores del XML.

Mostrá el plan y sus decisiones para revisión. No escribas código de la app todavía.
```

Revisá el plan: archivos, componentes, recursos, destinos y pruebas deben ser concretos. Una dependencia necesaria del piloto se incorpora de forma explícita, no de manera silenciosa.

## 6. Checklist, tareas y consistencia

Invocá cada skill por separado, revisando su resultado:

```text
/speckit-checklist

Generá una checklist de calidad UX y funcional del piloto: fidelidad a Inicio, estados reales, navegación, datos ausentes, confirmación de acciones, insets, texto ampliado y recursos definitivos. Diferenciá lo comprobable automáticamente de lo que requiere revisar capturas. No marques como verificado algo sin evidencia.
```

```text
/speckit-tasks

Descomponé el plan activo en tareas del piloto, en este orden: base compilable y comprobaciones existentes; recursos gráficos; tokens y componentes; Inicio y su navegación acordada; estados e integración; revisión visual y correcciones.

Cada tarea debe indicar resultado verificable. No agregues la implementación de las veinte vistas a este bloque ni cierres los pendientes de producto fuera de alcance.
```

```text
/speckit-analyze

Comprobá la coherencia entre constitución, especificación, plan, tareas y docs/ui-ux. Identificá contradicciones, requisitos omitidos y tareas que excedan el alcance. Separá las decisiones aplazadas y no modifiques código de la app.
```

Resolver inconsistencias del bloque antes de implementar. No hace falta completar en este bloque decisiones expresamente excluidas.

## 7. Implementar el bloque acordado

Después de revisar guía, especificación, plan y tareas:

```text
/speckit-implement

Implementá únicamente las tareas del piloto activo que acordamos. Conservá los cambios ajenos y el comportamiento existente.

Usá la imagen principal de Inicio y la guía común. Los recursos temporales y datos ficticios no cuentan como resultado final de producción.

Ejecutá las comprobaciones pertinentes definidas en el plan y explicá qué quedó realmente comprobado. Detenete al completar este bloque para revisar su captura en el Samsung S25 antes de extender el estilo a otras vistas.
```

La implementación debe informar archivos modificados, resultado de compilación y pruebas, capturas disponibles y diferencias conocidas. Compilar no acredita por sí solo similitud visual ni funcionamiento con el peluche.

## 8. Probar y comparar en el S25

Android Studio puede ejecutar el módulo en el teléfono. Con USB y depuración autorizada, adb también permite registrar el dispositivo. Referencia: [Android, ejecutar en un dispositivo](https://developer.android.com/studio/run/device).

En PowerShell, desde la raíz del repositorio:

```powershell
adb devices -l
```

Si hay varios dispositivos, usá el serial del S25. Sustituí el texto del ejemplo antes de ejecutar:

```powershell
$teoS25Serial = 'PEGAR_EL_SERIAL_REAL_DEL_S25'
adb -s $teoS25Serial shell getprop ro.build.version.release
adb -s $teoS25Serial shell wm size
adb -s $teoS25Serial shell wm density
adb -s $teoS25Serial shell settings get system font_scale
```

Para compilar y ejecutar las pruebas unitarias existentes:

```powershell
Push-Location '.\ProyectoAndroid'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
Pop-Location
```

Usar el JDK y el Gradle Wrapper del proyecto. Si la base ya falla por el entorno, registrar y resolver esa causa; no atribuirla al rediseño ni actualizar versiones automáticamente.

Instalar el APK debug resultante:

```powershell
adb -s $teoS25Serial install -r '.\ProyectoAndroid\app\build\outputs\apk\debug\app-debug.apk'
```

Abrí TEO y llegá a Inicio con una conexión válida. Las previews con fixtures sirven para reproducir estados, sin quitar los requisitos del primer uso en producción.

Para conservar una captura:

```powershell
New-Item -ItemType Directory -Path '.\docs\ui-ux\validacion' -Force
adb -s $teoS25Serial shell screencap -p /sdcard/teo-inicio.png
adb -s $teoS25Serial pull /sdcard/teo-inicio.png '.\docs\ui-ux\validacion\s25-inicio-despierto.png'
```

Compará la zona de app del boceto con la captura, considerando las barras del sistema y el ancho lógico. Revisar orden, alineación, tamaños relativos, personaje, colores, confeti, etiquetas, legibilidad y estado de los botones. La distinta altura de pantalla no debe resolverse estirando o comprimiendo arbitrariamente el contenido.

Pegá la captura en Cursor con una lista corta de diferencias concretas, por ejemplo:

```text
Compará esta captura del S25 con la referencia elegida de Inicio. Revisá composición, márgenes, tipografía, tarjetas, globo, personaje, confeti y navegación. Enumerá las diferencias observables y proponé correcciones acotadas sobre los componentes compartidos. No rediseñes la pantalla ni cambies sus funciones.
```

Finalmente:

```text
/speckit-converge

Contrastá el código del bloque activo con la especificación, el plan y las tareas. Considerá las comprobaciones y capturas disponibles. Si falta evidencia visual o de integración, indicá qué falta; no afirmes fidelidad o funcionamiento sin haberlo comprobado. Registrá las tareas pendientes y no inicies otra feature automáticamente.
```

## 9. Extender el trabajo por bloques

Una vez validada la base visual, crear otra especificación por bloque y repetir specify → clarify cuando haga falta → plan → tareas → análisis → implementación → comprobación.

| Bloque posterior | Referencias y alcance |
|---|---|
| Navegación y Mi TEO | P17–P20 y destinos todavía no incorporados al piloto; respetar las dependencias ya acordadas. |
| Rutinas | P07–P08; reutilizar formulario para crear/editar y diálogo de eliminar. |
| Contenido | P09–P13; música, envío, cuentos, PDF, edición y lectura disponible por API. |
| Agenda | P14–P16; datos locales, turnos y vacunas con las correcciones anotadas. |
| Primer uso y perfil | P01–P03; búsqueda, permisos, PIN, Wi-Fi y perfil; conservar nueve skins. |
| Actividad y Avisos | P05–P06; métricas y fechas de avisos, sin duplicar lo incorporado como dependencia del piloto. |

La constitución se mantiene una vez por proyecto y se revisa cuando cambian principios. No se crea una app nueva ni se reespecifica cada botón como una feature distinta.

El flujo de comandos se contrastó con el [quickstart oficial de Spec Kit](https://github.com/github/spec-kit/blob/main/docs/quickstart.md). Las revisiones humanas y la división en bloques de esta guía se adaptan a TEO y a la preferencia del usuario de resolver dudas antes de modificar comportamiento.
