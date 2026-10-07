package pe.tecsup.vynk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "JWT_SECRET=secreto-solo-para-pruebas-de-al-menos-32-caracteres")
class VynkApplicationTests {

	@Test
	void contextLoads() {
	}

}
