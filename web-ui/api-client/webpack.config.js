var path = require("path");
var config = {
    mode: "development",
    entry: ["idempotent-babel-polyfill", "./src/index.js"],
    output: {
        path: path.resolve(__dirname, "dist"),
        filename: "cytomine-client.js",
        libraryTarget: "umd"
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: [
                    {
                        loader: "babel-loader",
                        options: {
                            presets: ["env"]
                        }
                    },
                    "eslint-loader"
                ]
            }
        ]
    },
    resolve: {
        alias: {
            //"@": path.resolve(__dirname, "dist/cytomine-client.min.js")
            "@": path.resolve(__dirname, "src")
        }
    },
    devtool: "source-map"
};

module.exports = (env, argv) => {
    if (argv && argv.mode === "production") {
        config.output.filename = "cytomine-client.min.js";
    }
    return config;
};
