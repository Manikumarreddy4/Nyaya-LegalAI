module.exports = {
  timeout: 60000,
  ui: 'bdd',
  reporter: 'mochawesome',
  'reporter-option': [
    'reportDir=reports/mochawesome',
    'reportFilename=selenium-e2e-report',
    'html=true',
    'json=true',
    'overwrite=true'
  ],
  spec: ['tests/**/*.spec.js']
};
