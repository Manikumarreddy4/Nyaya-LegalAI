import { logger } from './logger.js';

export const gestureUtils = {
  async tap(client, element) {
    logger.info(`[Gesture] Performing Tap gesture`);
    if (!client) return;
    const location = await element.getLocation();
    const size = await element.getSize();
    const x = location.x + size.width / 2;
    const y = location.y + size.height / 2;
    
    await client.action('pointer')
      .move({ duration: 0, x, y })
      .down({ button: 0 })
      .up({ button: 0 })
      .perform();
  },

  async doubleTap(client, element) {
    logger.info(`[Gesture] Performing Double Tap gesture`);
    if (!client) return;
    const location = await element.getLocation();
    const size = await element.getSize();
    const x = location.x + size.width / 2;
    const y = location.y + size.height / 2;

    await client.action('pointer')
      .move({ duration: 0, x, y })
      .down({ button: 0 })
      .up({ button: 0 })
      .pause(100)
      .down({ button: 0 })
      .up({ button: 0 })
      .perform();
  },

  async longPress(client, element, durationMs = 1500) {
    logger.info(`[Gesture] Performing Long Press gesture for ${durationMs}ms`);
    if (!client) return;
    const location = await element.getLocation();
    const size = await element.getSize();
    const x = location.x + size.width / 2;
    const y = location.y + size.height / 2;

    await client.action('pointer')
      .move({ duration: 0, x, y })
      .down({ button: 0 })
      .pause(durationMs)
      .up({ button: 0 })
      .perform();
  },

  async swipe(client, startX, startY, endX, endY, duration = 800) {
    logger.info(`[Gesture] Performing Swipe gesture from (${startX}, ${startY}) to (${endX}, ${endY})`);
    if (!client) return;
    await client.action('pointer')
      .move({ duration: 0, x: startX, y: startY })
      .down({ button: 0 })
      .move({ duration, x: endX, y: endY })
      .up({ button: 0 })
      .perform();
  },

  async scroll(client, direction = 'down') {
    logger.info(`[Gesture] Performing Scroll gesture: ${direction}`);
    if (!client) return;
    const { width, height } = await client.getWindowSize();
    const startX = width / 2;
    const startY = direction === 'down' ? height * 0.8 : height * 0.2;
    const endY = direction === 'down' ? height * 0.2 : height * 0.8;
    await this.swipe(client, startX, startY, startX, endY, 600);
  },

  async dragAndDrop(client, sourceElement, targetElement) {
    logger.info(`[Gesture] Performing Drag and Drop`);
    if (!client) return;
    const sourceLoc = await sourceElement.getLocation();
    const sourceSize = await sourceElement.getSize();
    const sourceX = sourceLoc.x + sourceSize.width / 2;
    const sourceY = sourceLoc.y + sourceSize.height / 2;

    const targetLoc = await targetElement.getLocation();
    const targetSize = await targetElement.getSize();
    const targetX = targetLoc.x + targetSize.width / 2;
    const targetY = targetLoc.y + targetSize.height / 2;

    await client.action('pointer')
      .move({ duration: 0, x: sourceX, y: sourceY })
      .down({ button: 0 })
      .pause(500)
      .move({ duration: 1000, x: targetX, y: targetY })
      .up({ button: 0 })
      .perform();
  },

  async pinch(client) {
    logger.info(`[Gesture] Performing Pinch (zoom out) gesture`);
    if (!client) return;
    const { width, height } = await client.getWindowSize();
    const centerX = width / 2;
    const centerY = height / 2;

    // Dual finger action
    await client.actions([
      {
        type: 'pointer',
        id: 'finger1',
        actions: [
          { type: 'pointerMove', duration: 0, x: centerX - 100, y: centerY },
          { type: 'pointerDown', button: 0 },
          { type: 'pointerMove', duration: 800, x: centerX - 20, y: centerY },
          { type: 'pointerUp', button: 0 }
        ]
      },
      {
        type: 'pointer',
        id: 'finger2',
        actions: [
          { type: 'pointerMove', duration: 0, x: centerX + 100, y: centerY },
          { type: 'pointerDown', button: 0 },
          { type: 'pointerMove', duration: 800, x: centerX + 20, y: centerY },
          { type: 'pointerUp', button: 0 }
        ]
      }
    ]);
  },

  async zoom(client) {
    logger.info(`[Gesture] Performing Zoom (pinch open) gesture`);
    if (!client) return;
    const { width, height } = await client.getWindowSize();
    const centerX = width / 2;
    const centerY = height / 2;

    await client.actions([
      {
        type: 'pointer',
        id: 'finger1',
        actions: [
          { type: 'pointerMove', duration: 0, x: centerX - 20, y: centerY },
          { type: 'pointerDown', button: 0 },
          { type: 'pointerMove', duration: 800, x: centerX - 200, y: centerY },
          { type: 'pointerUp', button: 0 }
        ]
      },
      {
        type: 'pointer',
        id: 'finger2',
        actions: [
          { type: 'pointerMove', duration: 0, x: centerX + 20, y: centerY },
          { type: 'pointerDown', button: 0 },
          { type: 'pointerMove', duration: 800, x: centerX + 200, y: centerY },
          { type: 'pointerUp', button: 0 }
        ]
      }
    ]);
  }
};

export default gestureUtils;
