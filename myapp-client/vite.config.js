export default defineConfig(({ command, mode, isSsrBuild, isPreview }) => {
    if (command === 'serve') {
        return {
            "build": "development",
            cache: false           // dev specific config
        }
    } else {
        // command === 'build'
        return {
            // build specific config
            Option: {
                mode: "development"
            }
        }
    }
})