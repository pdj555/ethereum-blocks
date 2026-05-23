import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ethereum Block Explorer — a focused chain reader",
  description:
    "Inspect one Ethereum block or trace one address across a curated 100-block slice. Numbers exact, hex monospace, nothing the dataset cannot back.",
  metadataBase: new URL("https://ethereum-blocks.vercel.app"),
  openGraph: {
    type: "website",
    siteName: "Ethereum Block Explorer",
    title: "Ethereum Block Explorer — a focused chain reader",
    description:
      "Inspect one block or trace one address. Numbers exact, hex monospace, nothing the dataset cannot back.",
    images: ["/og.svg"]
  },
  twitter: {
    card: "summary_large_image",
    title: "Ethereum Block Explorer",
    description: "Inspect one block or trace one address. Numbers exact, hex monospace.",
    images: ["/og.svg"]
  },
  icons: { icon: "/favicon.svg" }
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#fafbfd" },
    { media: "(prefers-color-scheme: dark)", color: "#06070b" }
  ]
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var t=localStorage.getItem("theme");if(t==="dark"||(!t&&window.matchMedia("(prefers-color-scheme: dark)").matches)){document.documentElement.dataset.theme="dark";}}catch(e){}})();`
          }}
        />
      </head>
      <body>{children}</body>
    </html>
  );
}
