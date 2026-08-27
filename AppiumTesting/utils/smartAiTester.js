import { logger } from './logger.js';

export class SmartAiTester {
  constructor(client) {
    this.client = client;
    this.discoveredWidgets = [];
    this.generatedScenarios = [];
  }

  async analyzeCurrentScreen() {
    logger.info('[SmartAI] Analyzing mobile screen view hierarchy...');
    let pageSource = '';

    if (this.client) {
      try {
        pageSource = await this.client.getPageSource();
      } catch (err) {
        logger.error(`[SmartAI] Failed to fetch live screen hierarchy: ${err.message}`);
      }
    } else {
      logger.info('[SmartAI] [MOCK] Generating mock screen layout for signup/booking page...');
      pageSource = `
        <hierarchy>
          <node class="android.widget.EditText" resource-id="input_email" text="" />
          <node class="android.widget.EditText" resource-id="input_phone" text="" />
          <node class="android.widget.EditText" resource-id="input_password" text="" />
          <node class="android.widget.Button" resource-id="btn_register" text="Register" />
          <node class="android.widget.TextView" resource-id="txt_header" text="Nyaya Legal AI Registration" />
        </hierarchy>
      `;
    }

    this.discoverWidgets(pageSource);
    this.generateScenariosFromWidgets();
    return {
      widgets: this.discoveredWidgets,
      scenarios: this.generatedScenarios
    };
  }

  discoverWidgets(xmlSource) {
    logger.info('[SmartAI] Extracting form elements and clickable targets...');
    this.discoveredWidgets = [];

    // Parse resource-ids and widget classes using regex matches
    const nodeRegex = /<node[^>]*class="([^"]+)"[^>]*resource-id="([^"]*)"/g;
    let match;
    while ((match = nodeRegex.exec(xmlSource)) !== null) {
      const widgetClass = match[1];
      const resourceId = match[2];
      if (resourceId) {
        let type = 'Unknown';
        if (widgetClass.includes('EditText')) type = 'Input';
        else if (widgetClass.includes('Button')) type = 'Button';
        else if (widgetClass.includes('TextView')) type = 'Text';
        else if (widgetClass.includes('CheckBox')) type = 'Checkbox';
        else if (widgetClass.includes('RadioButton')) type = 'Radio';

        this.discoveredWidgets.push({ resourceId, type, widgetClass });
      }
    }

    logger.info(`[SmartAI] Auto-detected ${this.discoveredWidgets.length} interactive widgets on screen.`);
  }

  generateScenariosFromWidgets() {
    logger.info('[SmartAI] Automatically generating validation scenarios...');
    this.generatedScenarios = [];

    this.discoveredWidgets.forEach(widget => {
      if (widget.type === 'Input') {
        const fieldName = widget.resourceId.replace('input_', '');
        
        // Scenario 1: Empty input check
        this.generatedScenarios.push({
          id: `AI-VAL-${fieldName.toUpperCase()}-001`,
          widgetId: widget.resourceId,
          type: 'Validation',
          action: 'Leave empty',
          expectedMessage: `${fieldName} is a required field.`
        });

        // Scenario 2: Formatting checks based on common schemas
        if (fieldName.includes('email')) {
          this.generatedScenarios.push({
            id: `AI-VAL-EMAIL-002`,
            widgetId: widget.resourceId,
            type: 'Validation',
            action: 'Type invalid format (e.g. user@com)',
            expectedMessage: 'Email format is invalid.'
          });
        } else if (fieldName.includes('phone')) {
          this.generatedScenarios.push({
            id: `AI-VAL-PHONE-002`,
            widgetId: widget.resourceId,
            type: 'Validation',
            action: 'Type short number (e.g. 12345)',
            expectedMessage: 'Phone number must contain exactly 10 digits.'
          });
        } else if (fieldName.includes('password')) {
          this.generatedScenarios.push({
            id: `AI-VAL-PASS-002`,
            widgetId: widget.resourceId,
            type: 'Validation',
            action: 'Type short password (<6 chars)',
            expectedMessage: 'Password must contain at least 6 characters.'
          });
        }
      } else if (widget.type === 'Button') {
        this.generatedScenarios.push({
          id: `AI-NAV-${widget.resourceId.toUpperCase()}`,
          widgetId: widget.resourceId,
          type: 'Navigation',
          action: 'Tap button',
          expectedMessage: 'Triggers action submission and transition'
        });
      }
    });

    logger.info(`[SmartAI] Compiled ${this.generatedScenarios.length} E2E automation validation cases.`);
  }
}

export default SmartAiTester;
