# Plan: Mi TEO

## Enfoque

Las reglas puras viven fuera de Android: cuerpo parcial de volumen y brillo, borrador frente a valor confirmado, fase de conexión, perfil de red, IP manual y el movimiento del `OnboardingGate`. La interfaz reutiliza ViewBinding, los estilos de Inicio y el formulario de perfil.

La ampliación de Perfil y su validación están en `skins-plan.md`. `TeoCostumeCatalog` reúne las nueve parejas originales y sus encuadres; `TeoAppearance` observa y recuerda únicamente el disfraz confirmado. El guardado y la consulta de perfil actualizan esa apariencia; `ProfileAppearanceGate` evita revertirla con respuestas anteriores. El formulario y el selector se comparten con primer uso y vista previa.

`nav_config` pasa a ser el menú. Conexión, Ajustes, Diagnóstico, cambio de Wi-Fi y la edición del perfil son destinos nuevos. El primer uso del perfil y el setup BLE se conservan. La edición y los formularios de Mi TEO no redirigen al adulto cuando cambia el estado de conexión. El primer uso sí sigue bloqueando.

`postConfig` mantiene su firma para los usos que envían tiempo de uso. Volumen y brillo usan un envío parcial, sin `playtime_limit_minutes` ni `belly_wake_enabled`. El modo noche sigue en su endpoint.

## Vista previa

`MyTeoPreviewHost` infla los layouts de producción y guarda borradores en memoria. Los engranajes abren el menú. «Ver conexión» abre Conexión. No inicializa `RobotConnectionManager` ni escribe preferencias.

## Verificación

Pruebas de unidad de las reglas y del gate. Compilación debug. Si el S25 está desbloqueado, instalar sin borrar datos y comparar las capturas con los bocetos. No se despliega la Raspberry y no se da por confirmado un cambio que el peluche no respondió.
