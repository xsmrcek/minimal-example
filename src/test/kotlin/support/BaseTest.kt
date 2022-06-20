package support

import org.example.minimalexample.Application
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration

@Suppress("UNCHECKED_CAST")
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = [Application::class],
        properties = [
            "rabbit-messaging.ssl=false",

            "management.health.rabbit.enabled=false",
            "kotlinx.coroutines.debug = true",
        ]
)
@ContextConfiguration(initializers = [CustomApplicationContextInitializer::class])
internal abstract class BaseTest : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    protected fun getProperty(name: String) = applicationContext.getBean(Environment::class.java).getProperty(name)

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}