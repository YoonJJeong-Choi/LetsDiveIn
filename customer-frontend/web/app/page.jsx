import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import BannerCollection from "@/components/homes/home-1/BannerCollection";
import BannerCountdown from "@/components/homes/home-1/BannerCountdown";
import Collections from "@/components/homes/home-1/Collections";
import Features from "@/components/common/Features";
import Hero from "@/components/homes/home-1/Hero";
import Products from "@/components/common/Products3";
import Testimonials from "@/components/common/Testimonials";

export const metadata = {
  title: "Home || Let’s Dive In",
  description: "Let’s Dive In 공식 쇼핑몰",
};

export default function HomePage() {
  return (
    <>
      <Header1 />
      <Hero />
      <Collections />
      <Products />
      <BannerCollection />
      <BannerCountdown />
      <Testimonials />
      <Features />
      <Footer1 />
    </>
  );
}
