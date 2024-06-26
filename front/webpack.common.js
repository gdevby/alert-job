const path = require('path')

const MiniCssExtractPlugin = require('mini-css-extract-plugin');


const plugins = [
	new MiniCssExtractPlugin({
		chunkFilename: '[name].[hash].css'
	}),
];

module.exports = {
	plugins,
	entry: "./src/index.js",
	output: {
		path: path.resolve(__dirname, 'dist'),
		chunkFilename: '[name].[hash].js',
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