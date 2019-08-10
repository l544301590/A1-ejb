/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.repository;

import fit5192.zz.repository.UserRepository;
import fit5192.zz.repository.exceptions.NonexistentEntityException;
import fit5192.zz.repository.exceptions.PreexistingEntityException;
import fit5192.zz.repository.exceptions.RollbackFailureException;
import fit5192.zz.repository.entities.User_;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.enterprise.context.SessionScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.NamedQuery;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author Zheng Ru
 */
@Stateless
public class JPAUserRepositoryImpl implements UserRepository {
    private static final String PERSISTENCE_UNIT = "A1-commonPU";//what's the function of this field?
    private EntityManagerFactory entityManagerFactory;

    public JPAUserRepositoryImpl() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }
    
    @Override
    public void addUser(User_ user) throws  Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(user);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (searchUserById(user.getId()) != null) {
                throw new PreexistingEntityException("User " + user + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
    @Override
    public void removeUserById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            User_ user;
            try {
                user = entityManager.getReference(User_.class, id);
                user.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The user with id " + id + " no longer exists.", enfe);
            }
            entityManager.remove(user);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
    
    @Override
    public void updateUser(User_ user) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            user = entityManager.merge(user);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                int id = user.getId();
                if (searchUserById(id) == null) {
                    throw new NonexistentEntityException("The user with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
    
    @Override
    public User_ searchUserById(int id) throws Exception {    
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(User_.class, id);
        } finally {
            entityManager.close();
        }
    }
    
    //for register login
    @SessionScoped//登陆注册之后  sessionscope 存入的是输入还是输出？
    public List<User_> searchUserByEmail(String email){
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        Query query = entityManager.createNamedQuery("User_.searchUserByEmail");
        query.setParameter("email", email);
        return query.getResultList();
    }
    
    
    @Override
    public List<User_> getAllUsers() throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        return entityManager.createNamedQuery("User_.findAll").getResultList();
    }
    @Override
    public String register(User_ user) {
        //even if the database require the email to be unique, Early inspection can make problems detected earlier
        List<User_> users = searchUserByEmail(user.getEmail());
        if(users.size() > 0){
            return "This email has been registered";
        }            //some orher exception can be happen(else if)
        try {
            addUser(user);
        } catch (Exception ex) {
            ex.toString();
        }
        return String.valueOf( user.getLevel());
    }
    
    @Override
    public String login(User_ user) {
        List<User_> users=searchUserByEmail(user.getEmail());
        if(!users.get(0).getPassword().equals(user.getPassword())){
            return "wrong password,try again";
        } //some orher exception can be happen( can add else if)
        return String.valueOf( users.get(0).getLevel());
    }
    
    @Override
    public List<User_> SerachUserByAnyAttribute(User_ user) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        int id = user.getId();
        String email = user.getEmail(); 
        String nickname = user.getNickname(); 
        String password = user.getPassword(); 
        int level = user.getLevel();
        String lastName = user.getLastName();
        String firstName = user.getFirstName(); 
        String address = user.getAddress(); 
        String phone = user.getPhone();
        HashMap<String,Object> constraint=new HashMap<>();
        if(id!=0){
            constraint.put("id",id );
        }
        if(!isEmpty(email)){
            constraint.put("email",email );
        }
        if(!isEmpty(nickname)){
            constraint.put("nickname",nickname );
        }
        if(!isEmpty(password)){
            constraint.put("password",password );
        }
         if(level!=0){
            constraint.put("level",level );
        }
        if(!isEmpty(lastName)){
            constraint.put("lastName",lastName );
        }
        if(!isEmpty(firstName)){
            constraint.put("firstName",firstName );
        }
        if(!isEmpty(address)){
            constraint.put("address",address );
        }
        if(!isEmpty(phone)){
            constraint.put("phone",phone );
        }
        
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User_> query = criteriaBuilder.createQuery(User_.class);
        Root<User_> studentProfile = query.from(User_.class);
        List<Predicate> predicatesList = new ArrayList<>();
        
        for(Object key : constraint.keySet()) {
            String attr = (String)key;
            Object value = constraint.get(attr);
            predicatesList.add(criteriaBuilder.equal(studentProfile.get(attr), value));
        }
        query.where(predicatesList.toArray(new Predicate[predicatesList.size()]));
        TypedQuery<User_> q = entityManager.createQuery(query);
        
        return  q.getResultList();
    }
        
    public static boolean isEmpty(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0||str==" ") {
        return true;
    }
    for (int i = 0; i < strLen; i++) {
        if ((Character.isWhitespace(str.charAt(i)) == false)) {
            return false;
        }
    }
    return true;
    }
/*
    public List<User> findUserEntities(int maxResults, int firstResult) {
        return findUserEntities(false, maxResults, firstResult);
    }

    private List<User> findUserEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(User_.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public int getUserCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<User> rt = cq.from(User_.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
*/

   
}
