
sudo ln -sf /mnt/wslg/.X11-unix/X0 /tmp/.X11-unix/X0 && export DISPLAY=:0

# development build
mvn spring-boot:run



# production build
mvn clean package -Dvaadin.productionMode=true -Pproduction
target/start.sh

String dstName,
            String dstBic,
            String dstIban,
            BigDecimal btgValue,
            String endToEndId,
            String usage