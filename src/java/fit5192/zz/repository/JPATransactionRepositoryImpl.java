/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.repository;

import static fit5192.zz.repository.JPAUserRepositoryImpl.isEmpty;
import fit5192.zz.repository.entities.Product;
import fit5192.zz.repository.exceptions.NonexistentEntityException;
import fit5192.zz.repository.exceptions.PreexistingEntityException;
import fit5192.zz.repository.exceptions.RollbackFailureException;
import fit5192.zz.repository.entities.Transaction_;
import fit5192.zz.repository.entities.User_;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
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
public class JPATransactionRepositoryImpl implements TransactionRepository {

    private static final String PERSISTENCE_UNIT = "A1-commonPU";//what's the function of this field?
    private EntityManagerFactory entityManagerFactory;
    //private JPAUserRepositoryImpl userRepository;//这样写 每调用一次都会创建一个类,要不把JPAUserRepositoryImpl中的searchByEmail设为static

    public JPATransactionRepositoryImpl() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        //this.userRepository = new  JPAUserRepositoryImpl();
    }
    @Override
    public void addTransaction(Transaction_ tran) throws  Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(tran);
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (searchTransactionById(tran.getId()) != null) {
                throw new PreexistingEntityException("Transaction " + tran + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
     @Override
    public void removeTransactionById(int id) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Transaction_ tran;
            try {
                tran = entityManager.getReference(Transaction_.class, id);
                tran.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The transaction with id " + id + " no longer exists.", enfe);
            }
            entityManager.remove(tran);
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
    public void updateTransaction(Transaction_ tran) throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            tran = entityManager.merge(tran);//the error maybe exsist
            transaction.commit();
        } catch (Exception ex) {
            try {
                transaction.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                int id = tran.getId();
                if (searchTransactionById(id) == null) {
                    throw new NonexistentEntityException("The Transaction with id " + id + " no longer exists.");
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
    public Transaction_ searchTransactionById(int id) throws Exception {  
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(Transaction_.class, id);
        } finally {
            entityManager.close();
        }
    }
    
    @Override
    public List<Transaction_> getAllTransactions() throws Exception {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        return entityManager.createNamedQuery("Transaction.findAll").getResultList();
    }

    @Override
    public List<Transaction_> SerachTransactionByAnyAttribute(Transaction_ transaction) {
        //无论是传ID还是传什么都得调用JPATransactionRe...中的方法
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        int id = transaction.getId();
        Date date = transaction.getDate(); 
        List<Product> products = transaction.getProducts(); 
        User_ users = transaction.getUser();
        HashMap<String,Object> constraint=new HashMap<>();
        if(id!=0){
            constraint.put("id",id );
        }
        if(!isEmpty(date.toString())){//潜在问题，日期的默认值是什么？
            constraint.put("date",date );
        }
        if(!isEmpty(products.toString())){//List String后是什
            constraint.put("products",products );
        }
        if(!isEmpty(users.toString())){//List String后是什么？
            List<User_> usersAtrribute = null;
            constraint.put("user",users );//如何和数据库表格对应起来
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Transaction_> query = criteriaBuilder.createQuery(Transaction_.class);
        Root<Transaction_> resultTransactions = query.from(Transaction_.class);
        List<Predicate> predicatesList = new ArrayList<>();
        for(Object key : constraint.keySet()) {
            String attr = (String)key;
            Object value = constraint.get(attr);
            predicatesList.add(criteriaBuilder.equal(resultTransactions.get(attr), value));
        }
        query.where(predicatesList.toArray(new Predicate[predicatesList.size()]));
        TypedQuery<Transaction_> q = entityManager.createQuery(query);
        
        return  q.getResultList();
        
    }
    @Override
    public List<Transaction_> SearchTransactionsByUserId(int userId) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        Query query = entityManager.createNamedQuery("Transaction.SearchTransactionsByUserId");
        query.setParameter("userId", userId);
        return query.getResultList();
    }
/*
    public List<Transaction> findTransactionEntities(int maxResults, int firstResult) {
        return findTransactionEntities(false, maxResults, firstResult);
    }

    private List<Transaction> findTransactionEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Transaction_.class));
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

    public int getTransactionCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Transaction> rt = cq.from(Transaction_.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    */


}
