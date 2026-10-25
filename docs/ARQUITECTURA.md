# Arquitectura del motor VFX

## Objetivo

Construir un motor de efectos visuales en tiempo real para Forge 1.20.1 que permita combinar varias técnicas gráficas en una escena: shaders GLSL, geometría, materiales, partículas, estelas, animación temporal y posprocesamiento.

No se da por hecho que todos los efectos puedan resolverse con un único shader. Los shaders calculan cómo se dibujan los datos que reciben; el mod también debe crear geometría, gestionar recursos, actualizar animaciones y coordinar los efectos con el mundo.

## Capas

### 1. Núcleo común

Responsable de identificadores, definiciones, ciclo de vida, parámetros y estados de los efectos. No debe importar clases exclusivas del cliente como cámaras, matrices de renderizado o buffers gráficos.

### 2. Simulación

Actualiza el estado de un efecto con un paso de tiempo controlado. Cuando un efecto influya en jugadores, entidades o bloques, esa lógica debe tener una política clara de autoridad del servidor y validación multijugador.

### 3. Renderizado del cliente

Dibujará geometría personalizada, objetos orientados a cámara, anillos, mallas, estelas y partículas. Se dividirá en componentes reutilizables en vez de concentrar todo en un solo renderer.

### 4. Shaders y materiales

Los recursos GLSL se cargarán como recursos del mod. Cada shader deberá declarar sus entradas, uniformes, texturas, pases y requisitos gráficos. Los materiales podrán compartir parámetros como color, emisión, opacidad, distorsión y desplazamiento.

### 5. Posprocesamiento

Los efectos de pantalla deben integrarse en una cadena controlada de renderizado. Bloom, distorsión, desenfoque o cambios de color no se implementarán como si fueran simples partículas: requieren operar sobre una textura de la escena y respetar el ciclo de renderizado de Minecraft.

### 6. Línea de tiempo

Coordina fases y curvas temporales: preparación, aparición, intensidad máxima, transición y salida. Las animaciones deberían poder usar curvas normalizadas y valores reproducibles a partir del tiempo transcurrido.

### 7. Red

El servidor enviará únicamente el estado y los parámetros necesarios para sincronizar eventos que deban verse en otros clientes. Cada cliente realizará su propio renderizado. No se enviarán buffers gráficos ni se confiará al cliente la autoridad de las mecánicas que afectan al mundo.

## Flujo de un efecto

1. Se registra una fábrica con un identificador único.
2. Se crea una instancia y se inicializa su estado.
3. El runtime actualiza la simulación en el hilo de juego apropiado.
4. Los componentes cliente convierten el estado en datos visuales.
5. La escena final se dibuja mediante los pases necesarios.
6. Al terminar, el efecto libera sus recursos y se retira del runtime.

## Reglas para mantener el proyecto sano

- Evitar dependencias cliente en clases comunes para no romper servidores dedicados.
- No realizar operaciones costosas ni asignaciones masivas por fotograma sin medirlas.
- Mantener separados el estado de simulación y los objetos temporales del renderizado.
- No asumir que una distorsión de pantalla puede afectar a otros jugadores; eso exige sincronizar el evento y aplicar el pase en cada cliente.
- Gestionar correctamente recursos gráficos al cambiar de mundo, recargar recursos o cerrar el juego.
- Añadir cada nueva etapa detrás de una prueba o demostración pequeña y verificable.

## Estado de la implementación

El proyecto tiene el punto de entrada Forge, identificadores, un contrato inicial para efectos, un registro sencillo y un administrador de ciclo de vida. Todavía no se han implementado el renderer de VFX, el cargador de GLSL, la cadena de posprocesamiento ni un editor visual.
