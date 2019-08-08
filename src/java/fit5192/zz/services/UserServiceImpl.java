/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fit5192.zz.services;

import fit5192.zz.repository.JPAProductRepositoryImpl;
import fit5192.zz.repository.JPAUserRepositoryImpl;
import fit5192.zz.repository.UserRepository;
import fit5192.zz.repository.UserRepositoryFactory;
import fit5192.zz.repository.entities.Product;
import fit5192.zz.repository.entities.User_;
import java.util.List;
import javax.ejb.Stateless;

/**
 *
 * @author 10759
 */
@Stateless
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository = UserRepositoryFactory.getInstance();

    @Override
    public String register(User_ user) {
        //1 check email distinction
//        List<User> users=userRepository.searchUserByEmail(user.getEmail());
//        if(users.size()>0){
//            user.setLevel(1);
//            try {
//                userRepository.addUser(user);
//            } catch (Exception e) {
//                return "Unexpected error!";
//            }
//            return "The email address has been registered!";
//        }
//        return "1";
        try {
            userRepository.addUser(user);
            return "YYYYYYYYY";
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return "NNNNNNNNN";
    }
    
}
