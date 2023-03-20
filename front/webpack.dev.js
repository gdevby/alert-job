const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
	mode: 'development',
	devtool: 'source-map',
	watch: true,
	watchOptions: {
		aggregateTimeout: 500,
		poll: 1000
	},
	devServer: {
		hot: true,
	},
});