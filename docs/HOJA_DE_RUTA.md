# Hoja de ruta

La prioridad es construir un motor gráfico fiable y ampliarlo con componentes reutilizables. Esta lista distingue las bases que ya están en el repositorio de las funciones que todavía deben desarrollarse.

## Fase 1 — Proyecto y control de efectos
- [x] Proyecto Forge 1.20.1 con Java 17.
- [x] Compilación automática en GitHub Actions.
- [x] Contrato inicial, registro con metadatos y runtime local.
- [x] Comandos locales `/vfx list`, `enable`, `disable`, `toggle`, `status` y `disable all`.
- [x] Primer VFX de prueba con geometría de líneas.
- [x] Barras musicales de horizonte con animación procedural.
- [x] Aurora arcoíris en capas de geometría semitransparente.
- [ ] Probar ambos efectos visualmente en cliente y ajustar su apariencia.
- [x] Shader GLSL básico registrado y tipo de renderizado reutilizable.
- [x] Compilación Gradle verificada en CI para el código actual.
- [ ] Validar visualmente el shader y los comandos dentro del cliente de Minecraft.
- [ ] Probar el arranque en servidor dedicado.

## Fase 2 — Renderizado propio
- [ ] Establecer componentes reutilizables para mallas y objetos orientados a cámara.
- [ ] Crear materiales básicos con emisión, opacidad, desplazamiento y control de color.
- [ ] Gestionar recarga y liberación de todos los recursos gráficos.
- [ ] Añadir herramientas de diagnóstico cuando un shader no compile o falten recursos.

## Fase 3 — Herramientas de VFX
- [ ] API de materiales y parámetros reutilizables.
- [ ] Componentes de partículas y emisores.
- [ ] Estelas con control de longitud, anchura y transparencia.
- [ ] Curvas de animación, fases y transiciones.
- [ ] Carga de definiciones externas para efectos compuestos sencillos.

## Fase 4 — Posprocesamiento
- [ ] Prototipo aislado de un pase de pantalla.
- [ ] Infraestructura para distorsión, gradación de color y bloom.
- [ ] Ajustes de calidad y comportamiento seguro si un pase no está disponible.

## Fase 5 — Primeras demostraciones
- [ ] Meteorito con núcleo incandescente, estela y humo.
- [ ] Portal con anillos, distorsión y salida de una entidad visual.
- [ ] Fenómeno de agujero negro con disco de acreción y distorsión.
- [ ] Supernova con fases temporales y variación del cielo.
- [ ] Secuencias de cielo: auroras, nubes y cuerpos celestes.

## Fase 6 — Autoría y multijugador
- [ ] API de componentes para que otros efectos puedan reutilizarse.
- [ ] Mensajes de red para eventos visuales sincronizados.
- [ ] Mecánicas autoritativas del servidor para fuerzas, daño o cambios del mundo.
- [ ] Herramientas de depuración, perfilado y documentación para creadores.
- [ ] Editor visual de composición de efectos.

## Criterios de avance

No considerar una función terminada hasta que compile, se haya probado en el juego y tenga claro su comportamiento en servidor dedicado, recursos recargados y equipos de rendimiento limitado cuando corresponda.
