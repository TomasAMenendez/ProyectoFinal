# Mi TEO

## Qué se construye

Mi TEO reemplaza el destino temporal de Configuración. El engranaje de las pantallas principales abre el menú. «Ver conexión» de Inicio abre Conexión. El menú ofrece Ajustes de TEO, Conexión y Perfil del niño. Diagnóstico avanzado se abre desde Conexión, con menos protagonismo que las acciones principales. Estas pantallas son secundarias: tienen regreso, no muestran la navegación inferior y vuelven al origen.

La versión del pie sale de `BuildConfig`. La composición sigue los bocetos de Mi TEO, Ajustes, Conexión, Diagnóstico y Perfil, con la terminación de Inicio y Rutinas.

## Ajustes de TEO

La primera tarjeta muestra volumen máximo y brillo de la pantalla de TEO, con el porcentaje junto a cada deslizador, en rangos de 0 a 100 y pasos de 5. Aplicar cambios está dentro de esa tarjeta y envía solo esos dos campos. No modifica el límite de tiempo de uso ni el pulsador.

La segunda tarjeta es el modo noche del peluche, con su interruptor y una explicación breve. Se envía aparte, al cambiar el interruptor. Mientras los valores se cargan o si la consulta falla, no se muestran predeterminados como confirmados ni se pueden enviar. Un borrador en edición no se pisa con una actualización remota. Si el envío falla, el borrador se conserva y se muestra el resultado real. El modo noche del peluche es independiente del tema visual del teléfono.

## Conexión y diagnóstico

Conexión muestra el estado real de comunicación, la búsqueda con progreso indeterminado y la información de red disponible. El dato `current` es el perfil de NetworkManager, no necesariamente el nombre de la red. No se inventa una red.

Cambiar red Wi-Fi abre la lista, permite actualizarla, ocultar o mostrar la contraseña y trata las redes abiertas. Elegir una red no confirma la comunicación. Después del cambio se redescubre TEO. Ese cambio por HTTP no abre Bluetooth ni pide el PIN del primer uso. Sin comunicación HTTP, el camino es la recuperación BLE existente y el regreso vuelve al origen.

Diagnóstico avanzado separa la información de solo lectura de la conexión manual. Muestra estado, IP del teléfono, dirección de TEO y puerto efectivo. La IP escrita se distingue de la dirección guardada y de una comunicación comprobada. No se fija el puerto en 8080 si el descubrimiento configuró otro. No se muestran tokens ni contraseñas.

## Perfil del niño

Se reutilizan el modelo y el formulario compartidos. Nombre y género son obligatorios. Los textos opcionales admiten hasta 600 caracteres. El disfraz conserva las nueve opciones y sus identificadores. Desde Mi TEO se precarga el perfil, se puede volver y el botón dice «Guardar cambios». Guardar exige confirmación de TEO. Un error conserva lo escrito. La edición no activa el bloqueo del primer uso, no manda la app al fondo ni termina siempre en Inicio.

El primer uso sigue en «Guardar y continuar», con sus condiciones obligatorias. Una actualización de estado no saca al adulto de un formulario ni genera un bucle. Entrar a Agenda sin TEO queda como decisión aparte.

La ampliación del 15 de octubre incorpora las nueve parejas PNG entregadas en `docs/ui-ux/referencias/skins`. El selector muestra imagen de frente y nombre; la selección pendiente se confirma o cancela sin modificar el perfil guardado. El formulario usa etiquetas por encima de los campos, contadores legibles y opciones de género independientes que pueden envolver filas.

Los ejemplos de Nombre, Qué le gusta y preferencias se muestran como sugerencias del campo vacío, sin formar parte del texto editable ni del JSON. La vista previa comienza con el perfil vacío; al editar un perfil guardado se precargan sus datos reales o los confirmados en esa sesión simulada.

Después de un guardado completo confirmado por TEO, el personaje de Inicio usa la pose saludando del disfraz elegido. El catálogo común permite usar la pose correspondiente en cualquier imagen del personaje. Se conserva el logo oficial. El último disfraz confirmado se recuerda en este teléfono para abrir la app sin conexión. Un borrador, una respuesta fallida o incompleta y una consulta anterior al guardado no cambian esa apariencia. Al conectar se consulta el perfil, conservando los contratos existentes. No se cambia el aspecto físico del peluche ni sus ojos mediante esta personalización gráfica de Android.

## Vista previa

Usa los mismos layouts, con estado aislado. Permite recorrer las cinco pantallas, aplicar ajustes simulados, cambiar el modo noche, elegir estados de conexión, recorrer Wi-Fi ficticio, probar una IP y editar el perfil con las nueve skins. Las simulaciones se identifican. No consulta redes reales, no envía credenciales, no llama a TEO y no escribe preferencias de producción.

Guardar el perfil simulado actualiza también el personaje de Inicio en la sesión de vista previa. El perfil confirmado y el borrador se restauran al recrear la actividad; no se guardan como perfil real ni como preferencias de producción.
