# Tareas de entrega funcional

- [x] T001. Línea de base Android, lint, API Python y revisión de contratos.
- [x] T002. Corregir Agenda y recuperación del primer uso, con pruebas.
- [x] T003. Corregir persistencia/concurrencia de rutinas y reinicio diario, con pruebas.
- [x] T004. Corregir lectura, confirmaciones y nombres de archivos en contenido, con pruebas.
- [x] T005. Servicio de comunicación y permisos compatibles con Android 16.
- [x] T006. Compilación real por defecto; variante de vista previa independiente y proyecto portátil.
- [x] T007. Pruebas de la app real en S25 e integración local de API; revisar estados y errores.
- [x] T008. Paquete para Android Studio, APK y guía de preparación y prueba.
- [ ] T009. Verificar con TEO físico conexión BLE/LAN, audio, LCD/pictogramas, rutinas, ajustes y datos. No cerrar esta tarea sin evidencia física.

Resultado local: 213 pruebas unitarias debug, 215 preview (incluyen las comunes), 203 Python y 18 instrumentadas en S25 aprobadas. Lint sin errores; debug, preview y release compilan. Se corrigió además un fallo de arranque detectado al abrir la app real. El ZIP extraído compila y pasa sus pruebas sin rutas locales del proyecto. Evidencia y límites en docs/entrega/VALIDACION.md.

T009 conserva el bloqueo externo confirmado por el usuario: TEO está en otra casa y no existe acceso remoto. Se entrega actualización Python y recorrido físico para su amigo; no se desplegó ni se marca hardware probado.
