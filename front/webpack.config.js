const path = require('path')

const MiniCssExtractPlugin = require('mini-css-extract-plugin');


const plugins = [
	new MiniCssExtractPlugin({
		filename: '[name].css'
	}),
];

module.exports = {
	plugins,
	entry: "./src/index.js",
	output: {
		path: path.resolve(__dirname, 'dist'),
		clean: true,
	},
	module: {
		rules: [
			{ test: /\.js$/, exclude: /node_modules/, loader: "babel-loader" },
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
            },
		]
	},
	watch: true,
	watchOptions: {
		aggregateTimeout: 500,
		poll: 1000
	}
}