import type { Plugin } from 'vite'

const plugin = {
  name: 'remove-data-testid',
}

export function removeDataTestid(): Plugin {
  if (process.env.NODE_ENV !== 'production') {
    return plugin
  }
  return {
    ...plugin,
    // https://rollupjs.org/plugin-development/#transform
    transform(code: string, id: string) {
      if (/\.vue\?vue&type=script&setup=true&lang\.ts$/.test(id)) {
        return {
          code: code.replace(/"data-testid": "[^"]*",?/g, ''),
          map: null,
        }
      }
    },
  }
}
