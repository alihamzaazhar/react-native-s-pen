const path = require("path");
const { getDefaultConfig, mergeConfig } = require("@react-native/metro-config");

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, "..");

const config = {
  watchFolders: [workspaceRoot],
  resolver: {
    disableHierarchicalLookup: true,
    nodeModulesPaths: [path.resolve(projectRoot, "node_modules")],
    extraNodeModules: {
      react: path.resolve(projectRoot, "node_modules/react"),
      "react-native": path.resolve(projectRoot, "node_modules/react-native"),
    },
  },
};

module.exports = mergeConfig(getDefaultConfig(projectRoot), config);
