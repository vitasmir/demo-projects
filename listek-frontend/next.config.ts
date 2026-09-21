import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  turbopack: {
    root: __dirname,
  },
  async rewrites() {
    return [
      {
        source: "/api/backend/:path*",
        destination: `${process.env.BACKEND_URL || "http://localhost:8080"}/api/v1/:path*`,
      },
      {
        source: "/api/manager/:path*",
        destination: `${process.env.ADMIN_BACKEND_URL || "http://localhost:8090"}/api/admin/:path*`,
      },
    ];
  },
};

export default nextConfig;
