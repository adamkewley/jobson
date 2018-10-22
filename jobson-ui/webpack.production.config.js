const baseConfig = require('./webpack.config');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = Object.assign({}, baseConfig, {
	mode: 'production',
	devtool: 'source-map',
	optimization: {
		minimizer: [new UglifyJsPlugin()],
	},
});
