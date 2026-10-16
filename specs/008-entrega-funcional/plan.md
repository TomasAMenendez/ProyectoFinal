# Plan de entrega funcional

1. Tomar línea de base de compilación, pruebas Android, lint y contratos Python. Revisar independientemente contratos de API, lifecycle/navegación y permisos/servicios.
2. Corregir primero fallos que bloquean entrega o arriesgan datos: carga de Agenda, escrituras de rutinas, reinicio diario, estado de lectura, rutas de archivos y recuperación LAN tras BLE. Agregar pruebas que reproduzcan los fallos relevantes.
3. Ajustar el servicio de comunicación a su tipo Android correcto. Separar la variante de vista previa y eliminar la ruta fija del JDK del proyecto portátil.
4. Probar la app real en el S25, con pruebas instrumentadas y un servidor local que ejecute los contratos reales sin activar hardware. Mantener la configuración y datos reales del teléfono. Etiquetar la evidencia simulada.
5. Preparar paquete limpio y guía de uso, actualizar la matriz de verificación y repetir las comprobaciones afectadas por correcciones.

La revisión de contratos y las correcciones de lifecycle/servicios se ejecutan en archivos distintos. La integración, Gradle, empaquetado y dispositivo se coordinan desde una sola ejecución. No repetir trabajos ajenos ni sobrescribir cambios locales.
