const path = require('path')

const { InjectManifest } = require('workbox-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');


const plugins = [
	new MiniCssExtractPlugin({
		filename: '[name].css'
	}),
	new InjectManifest({
		swSrc: './src/service-worker.js',
	})
];

module.exports = {
	plugins,
	entry: "./src/index.js",
	output: {
		path: path.resolve(__dirname, 'dist'),
		clean: true,
	},
	optimization: {
		usedExports: true,
	},
	module: {
		rules: [
			{
				test: /\.js$/, exclude: /node_modules/, use: {
					loader: 'babel-loader',
					options: {
						cacheDirectory: true,
					},
				},
			},
			{
				test: /\.(s[ac]|c)ss$/i,
				use: [
					MiniCssExtractPlugin.loader,
					'css-loader',
					'postcss-loader',
					'sass-loader',
				],
			},
			{
				exclude: "/node_modules/",
				test: /\.jpg$/,
				use: ['file-loader'],
			}
		]
	}
}