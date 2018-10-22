const path = require('path');
const fs = require('fs');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');


const apiPrefix = JSON.parse(fs.readFileSync(__dirname + "/src/resources/config.json", "utf8"))["apiPrefix"];

module.exports = {
	mode: 'development',
	target: 'web',

	entry: {
		"jobson-ui-js": './src/ts/index.tsx'
	},
	output: {
		path: path.resolve(__dirname, 'target/site'),
	},


	resolve: {
		extensions: ['.ts', '.tsx', '.js'],
	},

	module: {
		rules: [
			{ test: /.tsx?$/, use: 'ts-loader' },
			{ test: /.css$/, use: ['style-loader', 'css-loader'] },
			{ test: /\.(png|svg|jpg|gif)$/, use: ['file-loader'] },
			{ test: /\.(woff|woff2|eot|ttf|otf)$/, use: ['file-loader'] },
		],
	},

	devtool: process.env.WEBPACK_DEVTOOL || 'source-map',
	devServer: {
		host: "0.0.0.0",
		port: "8090",

		historyApiFallback: true,
		proxy: {
			[apiPrefix]: {
				target: "http://localhost:8080",
				pathRewrite: {
					["^" + apiPrefix]: "/"
				},
				secure: false,
				ws: true,
			}
		}
	},

	plugins: [
		new HtmlWebpackPlugin({template: './src/html/index.html'}),
		new CopyWebpackPlugin(["src/resources/config.json"]),
	],
};
