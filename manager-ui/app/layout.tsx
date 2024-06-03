import type {Metadata} from "next";
import {Inter} from "next/font/google";
import "./globals.css";
import {AntdRegistry} from "@ant-design/nextjs-registry";
import GlobalContainer from "@/components/global-container";
import bg from '@/public/global-background.jpg'

const inter = Inter({subsets: ["latin"]});

export const metadata: Metadata = {
    title: "Dynamic Job"
}

export default function RootLayout({
                                       children,
                                   }: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="en">
        <body className={inter.className}
              style={{backgroundImage: `url(${bg.src})`, opacity: 0.95, backgroundSize: 'cover'}}>
        <AntdRegistry>
            <GlobalContainer>{children}</GlobalContainer>
        </AntdRegistry>
        </body>
        </html>
    );
}
