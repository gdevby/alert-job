const path = require('path')

module.exports = {
	entry: "./src/index.js",
	output: {
		path: path.join(__dirname, "dist", "assets"),
		filename: "bundle.js"
	},
	module: {
		rules: [{ test: /\.js$/, exclude: /node_modules/, loader: "babel-loader" }]
	},
	watch: true,
	watchOptions: {
		aggregateTimeout: 500,
		poll: 1000 // порверяем измемения раз в секунду
	}
}