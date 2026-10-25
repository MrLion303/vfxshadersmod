# Hoja de ruta

La prioridad es construir un motor gráfico fiable y ampliarlo con componentes reutilizables. Esta lista distingue las bases que ya están en el repositorio de las funciones que todavía deben desarrollarse.

## Fase 1 — Proyecto y control de efectos
- [x] Proyecto Forge 1.20.1 con Java 17.
- [x] Compilación automática en GitHub Actions.
- [x] Contrato inicial, registro con metadatos y runtime local.
- [x] Categorías funcionales para ambiente, instancias colocadas, entidades, pantalla, partículas, audio y simulación.
- [x] Comandos locales /vfx list, enable, disable, toggle, status y disable all.
- [x] Primer VFX de prueba con geometría de líneas.
- [x] Barras musicales de horizonte con animación procedural.
- [x] Cierre geométrico continuo para las barras musicales y la aurora.
- [x] Aurora arcoíris en capas de geometría semitransparente.
- [x] VFX de cielo arcoíris cambiante.
- [x] Modelo de instancias colocadas con dimensión, posición, orientación, escala, duración y parámetros.
- [x] Almacén que permite múltiples instancias del mismo tipo y las limpia al abandonar la sesión.
- [x] Contexto común de cámara y registro de renderers de mundo por etapa.
- [x] Shader GLSL básico y tipos de renderizado reutilizables.
- [x] Agujero negro con horizonte negro, disco de acreción y tamaño personalizable de 1 a 100 bloques.
- [x] Comando para colocar agujeros negros de 10 bloques por defecto o elegir otro diámetro.
- [x] Pase de lente gravitacional que deforma la imagen del mundo alrededor de cada agujero negro visible.
- [ ] Probar visualmente todos los efectos dentro del cliente y ajustar su apariencia.
- [ ] Verificar el pase de lente en distintos drivers, resoluciones y modos de renderizado.
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
- [x] Contexto de framebuffer y pase GLSL usado para la lente gravitacional.
- [x] Gestionar el target auxiliar ante cambios de resolución y recargas de shaders.
- [ ] Implementar pases adicionales para gradación de color, aberración cromática y bloom.
- [ ] Ajustes de calidad y comportamiento seguro si un pase no está disponible.

## Fase 5 — Primeras demostraciones
- [ ] Meteorito con núcleo incandescente, estela y humo.
- [ ] Portal con anillos, distorsión y salida de una entidad visual.
- [x] Fenómeno de agujero negro con disco de acreción, anclaje en coordenadas y lente gravitacional.
- [ ] Supernova con fases temporales y variación del cielo.
- [ ] Secuencias de cielo: auroras, nubes y cuerpos celestes.

## Fase 6 — Autoría y multijugador
- [ ] Mensajes de red para eventos visuales sincronizados.
- [ ] Mecánicas autoritativas del servidor para fuerzas, daño o cambios reales en el mundo.
- [ ] Herramientas de depuración, perfilado y documentación para creadores.
- [ ] Editor visual de composición de efectos.

## Criterios de avance

No considerar una función terminada hasta que compile, se haya probado en el juego y tenga claro su comportamiento en servidor dedicado, recursos recargados y equipos de rendimiento limitado cuando corresponda.
