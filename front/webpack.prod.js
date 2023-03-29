const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
	mode: 'production',
	devtool: false,
	optimization: {
		minimize: true,
		splitChunks: {
			minSize: 10000,
			maxSize: 250000,
		},
	},
	performance: {
		hints: false,
	},
});