package it.cnr.iasi.saks.inspection.test;

import it.cnr.iasi.saks.inspection.test.sampleApplication.Application;
import it.cnr.iasi.saks.inspection.test.sampleHibernate.HibernateUtil;
import it.cnr.iasi.saks.inspection.test.sampleHibernate.dto.EmployeeEntity;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class HibernateTest {
    @Test
    public void testHibernate() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();

        //Add new Employee object
        EmployeeEntity emp = new EmployeeEntity();
        emp.setEmail("demo-user@mail.com");
        emp.setFirstName("demo");
        emp.setLastName("user");

        session.save(emp);

        session.getTransaction().commit();
        HibernateUtil.shutdown();
    }
}
