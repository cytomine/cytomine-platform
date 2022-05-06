var path = require('path');
var config = {
  mode: 'development',
  entry: './src/index.js',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'cytomine-client.js',
    libraryTarget: 'umd'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/env'],
              plugins: ['@babel/plugin-proposal-object-rest-spread']
            }
          },
          'eslint-loader'
        ]
      }
    ]
  },
  resolve: {
    alias: {
      //"@": path.resolve(__dirname, "dist/cytomine-client.min.js")
      '@': path.resolve(__dirname, 'src')
    }
  },
  devtool: 'source-map'
};

module.exports = (env, argv) => {
  if (argv && argv.mode === 'production') {
    config.output.filename = 'cytomine-client.min.js';
  }
  return config;
};
