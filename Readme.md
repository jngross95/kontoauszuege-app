
sudo ln -sf /mnt/wslg/.X11-unix/X0 /tmp/.X11-unix/X0 && export DISPLAY=:0
mvn spring-boot:run



String dstName,
            String dstBic,
            String dstIban,
            BigDecimal btgValue,
            String endToEndId,
            String usage