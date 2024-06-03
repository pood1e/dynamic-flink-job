/** @type {import('next').NextConfig} */
const rewrites = () => {
    return [
        {
            source: "/api/:slug*",
            destination: "http://localhost:8080/:slug*",
        },
    ];
};

const nextConfig = {
    reactStrictMode: true,
    transpilePackages: ['antd'],
    webpack: (config) => {
        config.module.rules.push({
            test: /\.svg$/,
            use: ['@svgr/webpack'],
        });
        return config;
    },
    rewrites
};

export default nextConfig;
