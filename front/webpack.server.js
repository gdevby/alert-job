const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');


module.exports = merge(common, {
	mode: 'development',
	devtool: 'source-map',
	watch: true,
	plugins: [
		new HtmlWebpackPlugin({
				template: '../alert-job-gateway/src/main/resources/static/index.html',
         filename: 'index.html',
         inject: 'body',
  	}),
	],
	output: {
		publicPath: '/'
	},
	watchOptions: {
		aggregateTimeout: 500,
		poll: 1000
	},
	devServer: {
		static: {
			directory: path.join(__dirname, 'dist'),
		},
		hot: true,
		historyApiFallback: true,
		allowedHosts: "all",
		port: 3000,
	},
});