# Plan: Primer uso y recuperación de conexión

Se trabaja en la rama actual, sin commit y sin modificar el peluche, el servidor ni la composición de Inicio, Rutinas, Contenido, Agenda y Mi TEO.

Encontrar TEO sigue en `nav_onboarding_retry` y el provisionamiento BLE en `nav_wifi_setup`. El cambio HTTP sigue en `nav_wifi_change`. El perfil de primer uso sigue en `nav_child_profile`, con `ChildProfileForm` y el selector gráfico. `OnboardingGate` no cambia de decisión: sin red local, Bluetooth lleva al PIN y la red local lleva al perfil o a Inicio.

`FindTeoRules` separa búsqueda, sin resultado, Bluetooth apagado y permisos. `WifiStepRules` impide mostrar PIN y redes a la vez, y el chip de Bluetooth solo en el recorrido BLE. `WifiDraft` conserva la contraseña si la red elegida sigue presente. La fila de red es compartida y marca selección, no conexión.

TEO Vista previa infla esos layouts. Sus ejemplos no abren Bluetooth ni llaman a la API.
